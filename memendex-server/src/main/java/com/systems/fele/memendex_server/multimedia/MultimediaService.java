package com.systems.fele.memendex_server.multimedia;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.ffmpeg.swscale.SwsFilter;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.swscale.*;
import static org.bytedeco.javacpp.Pointer.malloc;

@Service
public class MultimediaService {

    /**
     * Generates a thumbnail in JPEG format.
     * @param inputFile The input file
     * @param outputFile The output file. The file extension will not be taken in
     *                   consideration to which format will be used.
     * @param overwrite If false and the file already exists, do nothing.
     */
    public static void generateThumbnail(File inputFile, File outputFile, boolean overwrite) {
        if (outputFile.exists() && !overwrite) return;

        try (var fmtCtx = avformat.avformat_alloc_context()) {
            var streamIndex = openAndDetectDecoder(inputFile.getAbsolutePath(), fmtCtx);
            var stream = fmtCtx.streams(streamIndex);
            try (var codec = avcodec.avcodec_find_decoder(stream.codecpar().codec_id())) {
                try (var codecCtx = openCodec(codec, stream)) {
                    try (var thumbnailFrame = generateThumbnailFromFirstFrame(fmtCtx, codecCtx, streamIndex);) {
                        encodeAs(thumbnailFrame, AV_CODEC_ID_MJPEG, outputFile);
                    }
                }
            }
        }
    }

    /**
     * Fetches the first frame from format context and calls resizeFrame().
     *
     * @param fmtCtx The format context
     * @param codecCtx The codec context
     * @param streamIndex The stream index that will be processed. The frames from other streams will be ignored.
     * @return The resized AVFrame
     */
    public static AVFrame generateThumbnailFromFirstFrame(AVFormatContext fmtCtx, AVCodecContext codecCtx, int streamIndex) {
        try (AVPacket packet = avcodec.av_packet_alloc(); AVFrame frame = avutil.av_frame_alloc()) {

            // Read frames until we find the frame of the desired stream
            while (avformat.av_read_frame(fmtCtx, packet) == 0) {
                if (packet.stream_index() == streamIndex) {
                    var isFrameReady = false;
                    while (!isFrameReady) {
                        int ret = avcodec_send_packet(codecCtx, packet);
                        if (ret < 0) {
                            throw new RuntimeException("Error reading frame from file: avcodec_send_packet() returned " + ret);
                        }

                        ret = avcodec_receive_frame(codecCtx, frame);
                        if (ret == 0)
                            isFrameReady = true;
                        else if (ret < 0 && ret != avutil.AVERROR_EAGAIN()) {
                            throw new RuntimeException("Error reading frame from file: avcodec_receive_frame() returned " + ret + "(" + avErrorToString(ret) + ")");
                        }
                    }

                    return resizeFrame(codecCtx, frame);
                }
            }
        }

        throw new RuntimeException("Could not find frames to read from input");
    }

    private static String avErrorToString(int avError) {
        String str = "";
        try (Pointer buffer = malloc(256)) {
            BytePointer byteBuffer = buffer.getPointer(BytePointer.class);
            av_strerror(avError, byteBuffer, 256);
            str = byteBuffer.getString();
        }
        return str;
    }

    /**
     * Encodes the frame using the desired codec
     *
     * @param thumbnailFrame The frame to be encoded
     * @param codecId Id of the codec to use as encoder (passed to {@link avcodec#avcodec_find_encoder}
     * @param output The output file
     */
    private static void encodeAs(AVFrame thumbnailFrame, int codecId, File output) {
        try (AVCodec codec = avcodec_find_encoder(codecId)) {
            AVCodecContext codecCtx = avcodec_alloc_context3(codec);

            var timeBase = new AVRational();
            timeBase.num(1);
            timeBase.den(25);

            codecCtx.bit_rate(400000);
            codecCtx.width(thumbnailFrame.width());
            codecCtx.height(thumbnailFrame.height());
            codecCtx.time_base(timeBase);
            codecCtx.pix_fmt(thumbnailFrame.format());
            codecCtx.color_range(AVCOL_RANGE_JPEG);

            avcodec_open2(codecCtx, codec, (AVDictionary) null);

            try (AVPacket pkt = avcodec.av_packet_alloc()) {
                avcodec_send_frame(codecCtx, thumbnailFrame);
                if (avcodec_receive_packet(codecCtx, pkt) == 0) {
                    try (var fos = new FileOutputStream(output)) {
                        getToStream(pkt.data(), pkt.size(), fos);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Copies a buffer from BytePointer into a outputstream
     * @param data The input buffer
     * @param size Size of the buffer. This is not checked.
     * @param out The output stream
     * @throws IOException If any errors
     */
    private static void getToStream(BytePointer data, int size, OutputStream out) throws IOException {
        byte[] buffer = new byte[256];
        int offset = 0;

        while (offset < size) {
            int len = Math.min(size - offset, buffer.length);
            data.get(buffer, 0, len);
            out.write(buffer, 0, len);
            offset += len;
            data = data.getPointer(len);
        }
    }

    /**
     * If pixFmt is a deprecated format, returns an appropriate
     * format to it.
     *
     * @param pixFmt The pixel format
     * @return The appropriate pixel format
     */
    private static int promoteToPreferredPixelFormat(int pixFmt) {
        return switch (pixFmt) {
            case AV_PIX_FMT_YUVJ420P -> AV_PIX_FMT_YUV420P;
            case AV_PIX_FMT_YUVJ440P -> AV_PIX_FMT_YUV440P;
            case AV_PIX_FMT_YUVJ444P -> AV_PIX_FMT_YUV444P;
            default -> pixFmt;
        };
    }

    private static AVFrame resizeFrame(AVCodecContext codecCtx, AVFrame inputFrame) {
        // Calculate appropriate size for a target width of 320 pixels;
        int thumbWidth = 320;
        int thumbHeight = inputFrame.height() * thumbWidth / inputFrame.width();

        if (thumbHeight % 2 == 1) thumbHeight -= 1;

        int sourcePixFormat = promoteToPreferredPixelFormat(codecCtx.pix_fmt());
        int targetPixFormat = AV_PIX_FMT_YUV420P;

        var swsCtx = sws_getContext(inputFrame.width(), inputFrame.height(), sourcePixFormat,
                thumbWidth, thumbHeight, targetPixFormat,
                SWS_FAST_BILINEAR, (SwsFilter) null, (SwsFilter) null, (DoublePointer) null);

        var dummy = new IntPointer(4);
        var srcRange = new IntPointer(1);
        var dstRange = new IntPointer(1);
        var brightness = new IntPointer(1);
        var contrast = new IntPointer(1);
        var saturation = new IntPointer(1);
        int ret = sws_getColorspaceDetails(swsCtx, dummy, srcRange, dummy, dstRange, brightness, contrast, saturation);
        // System.out.println("srcRange = " + srcRange.get(0) + ", dstRange = " + dstRange.get(0) + ", brightness = " + brightness.get(0) + ", contrast = " + contrast.get(0) + ", saturation = " + saturation.get(0));
        if (ret < 0) throw new RuntimeException("sws_getColorspaceDetails() returned " + ret);
        try (var coefs = sws_getCoefficients(SWS_CS_DEFAULT)) {
            srcRange.put(0, 0);
            ret = sws_setColorspaceDetails(swsCtx, coefs, srcRange.get(0), coefs, dstRange.get(0), brightness.get(0), contrast.get(0), saturation.get(0));
            if (ret < 0) throw new RuntimeException("sws_setColorspaceDetails() returned " + ret);
        }

        try (AVFrame thumbFrame = avutil.av_frame_alloc()) {
            thumbFrame.width(thumbWidth);
            thumbFrame.height(thumbHeight);
            thumbFrame.format(targetPixFormat);
            thumbFrame.color_range(AVCOL_RANGE_JPEG);

            av_image_alloc(thumbFrame.data(), thumbFrame.linesize(), thumbFrame.width(), thumbFrame.height(), targetPixFormat, 32);
            sws_scale(swsCtx, inputFrame.data(), inputFrame.linesize(), 0, inputFrame.height(), thumbFrame.data(), thumbFrame.linesize());

            return thumbFrame;
        }
    }

    private static AVCodecContext openCodec(AVCodec codec, AVStream stream) {
        var codecCtx = avcodec.avcodec_alloc_context3(codec);
        avcodec.avcodec_parameters_to_context(codecCtx, stream.codecpar());
        int ret = avcodec.avcodec_open2(codecCtx, codec, (AVDictionary) null);
        if (ret < 0)
            throw new RuntimeException("Error opening codec context: avcodec_open2() returned " + ret);
        return codecCtx;
    }

    public static int openAndDetectDecoder(String path, AVFormatContext fmtCtx) {
        int ret = avformat.avformat_open_input(fmtCtx, path, null, null);
        if (ret < 0)
            throw new RuntimeException("Could not open file " + path);

        // This function reads some bytes to detect the format
        // as some formats does not contain a proper header
        ret = avformat.avformat_find_stream_info(fmtCtx, (AVDictionary) null);
        if (ret < 0)
            throw new RuntimeException("Error detecting decoder: avformat_find_stream_info() returned " + ret);

        int stream_index = avformat.av_find_best_stream(fmtCtx, AVMEDIA_TYPE_VIDEO, -1, -1, (AVCodec) null, 0);
        if (stream_index == AVERROR_STREAM_NOT_FOUND)
            throw new RuntimeException("Error detecting decoder: AVERROR_STREAM_NOT_FOUND");
        else if (stream_index == AVERROR_DECODER_NOT_FOUND)
            throw new RuntimeException("Error detecting decoder: AVERROR_DECODER_NOT_FOUND");

        return stream_index;
    }

    public static int countFrames(AVFormatContext fmtCtx, int streamIndex, AVCodecContext codecCtx) {
        int count = 0;
        try (AVPacket packet = avcodec.av_packet_alloc(); AVFrame frame = avutil.av_frame_alloc()) {
            while (avformat.av_read_frame(fmtCtx, packet) >= 0) {
                if (packet.stream_index() == streamIndex) {
                    avcodec_send_packet(codecCtx, packet);
                    while (avcodec_receive_frame(codecCtx, frame) == 0) count++;
                }
            }
        }
        return count;
    }

    static void copy(AVFrame inputFrame, int inputPixFmt, AVFrame outputFrame, int outputPixFmt) {
        var swsCtx = sws_getContext(inputFrame.width(), inputFrame.height(), inputPixFmt,
                outputFrame.width(), outputFrame.height(), outputPixFmt,
                SWS_BILINEAR, (SwsFilter) null, (SwsFilter) null, (DoublePointer) null);

        //int ret = avutil.av_image_fill_arrays(outputFrame.data(), outputFrame.linesize(), new BytePointer(inputFrame.data().getPointer(0)), inputPixFmt, inputFrame.width(), inputFrame.height(), 32);
        //if (ret < 0)
        //    throw new RuntimeException("av_image_fill_arrays failed");
        sws_scale(swsCtx, inputFrame.data(), inputFrame.linesize(), 0, inputFrame.height(), outputFrame.data(), outputFrame.linesize());
    }

    public static void encode(AVFrame inputFrame) {
        try (AVCodec codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG)) {
            AVCodecContext codecCtx = avcodec_alloc_context3(codec);

            codecCtx.bit_rate(400000);
            codecCtx.width(inputFrame.width());
            codecCtx.height(inputFrame.height());
            var timeBase = new AVRational();
            timeBase.num(1);
            timeBase.den(25);
            codecCtx.time_base(timeBase);
            codecCtx.pix_fmt(AV_PIX_FMT_YUVJ420P);
            avcodec_open2(codecCtx, codec, (AVDictionary) null);

            try (AVFrame outputFrame = avutil.av_frame_alloc()) {
                outputFrame.format(codecCtx.pix_fmt());
                outputFrame.width(codecCtx.width());
                outputFrame.height(codecCtx.height());
                av_image_alloc(outputFrame.data(), outputFrame.linesize(), outputFrame.width(), outputFrame.height(), codecCtx.pix_fmt(), 32);
                copy(inputFrame, inputFrame.format(), outputFrame, codecCtx.pix_fmt());

                try (AVPacket pkt = avcodec.av_packet_alloc()) {
                    int gotOutput = 0;
                    avcodec_send_frame(codecCtx, outputFrame);
                    if (avcodec_receive_packet(codecCtx, pkt) == 0) {
                        try (var fos = new FileOutputStream("converted.jpeg")) {
                            for (int i = 0; i < pkt.size(); i++) {
                                fos.write(pkt.data().get(i));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

        }

    }

}
