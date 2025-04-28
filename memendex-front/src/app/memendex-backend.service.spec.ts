import { TestBed } from '@angular/core/testing';

import { MemendexBackendService } from './memendex-backend.service';

describe('MemendexBackendService', () => {
  let service: MemendexBackendService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MemendexBackendService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
