import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('ww_token');
  return next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request);
};

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';
  get<T>(path: string) { return this.http.get<T>(`${this.baseUrl}${path}`); }
  post<T>(path: string, body: unknown) { return this.http.post<T>(`${this.baseUrl}${path}`, body); }
  patch<T>(path: string, body: unknown) { return this.http.patch<T>(`${this.baseUrl}${path}`, body); }
}