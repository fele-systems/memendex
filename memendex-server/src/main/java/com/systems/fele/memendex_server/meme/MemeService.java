package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
public class MemeService {
    private final MemendexProperties memendexProperties;

    public MemeService(MemendexProperties memendexProperties) {
        this.memendexProperties = memendexProperties;
    }

    public BufferedImage getImage(Meme meme) throws IOException {
        return ImageIO.read(new File(memendexProperties.uploadLocation(), meme.fileName()));
    }

    public BufferedImage generateThumbnail(Meme meme) throws IOException {
        var img = ImageIO.read(new File(memendexProperties.uploadLocation(), meme.fileName()));
        double ratio = (double) img.getHeight() / (double) img.getWidth();
        int desiredHeight = 100;
        int desiredWidth = (int) (desiredHeight / ratio);
        BufferedImage bi = getCompatibleImage(desiredWidth, desiredHeight);
        // var bi = img;
        Graphics2D g2d = bi.createGraphics();

        double xScale = (double) desiredWidth / img.getWidth();
        double yScale = (double) desiredHeight / img.getHeight();

        AffineTransform at = AffineTransform.getScaleInstance(xScale,yScale);
        g2d.drawRenderedImage(img, at);
        g2d.dispose();
        return bi;
    }

    private BufferedImage getCompatibleImage(int w, int h) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        return gc.createCompatibleImage(w, h);
    }

}
