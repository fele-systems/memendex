export interface PaginatedResponse<T> {
  data: T[];
  count: number;
  totalCount: number;
  pageSize: number;
  page: number;
  hasNext: boolean;
}
