import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';

type Section = 'overview' | 'claims' | 'finance' | 'garage';
interface SessionUser { id: number; name: string; email: string; role: string; token: string; }
interface Vehicle { id: number; vin: string; make: string; model: string; year: number; currentMileage: number; }
interface Plan { id: number; name: string; coveredParts: string; durationMonths: number; kmLimit: number; price: number; }
interface Warranty { id: number; vehicleId: number; vehicle: string; plan: string; startDate: string; endDate: string; status: string; }
interface Claim { id: number; warrantyId: number; vehicle: string; part: string; description: string; costEstimate: number; status: string; validationNote: string; reviewNote: string | null; createdAt: string; }
interface FinanceApplication { id: number; type: string; vehicle: string; requestedAmount: number; tenureMonths: number; status: string; decisionNote: string; score: number | null; riskBand: string | null; annualRate: number | null; monthlyEmi: number | null; }
interface Installment { id: number; applicationId: number; installmentNo: number; dueDate: string; amount: number; lateFee: number; status: string; paidAt: string | null; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly sections: { id: Section; label: string; key: string }[] = [
    { id: 'overview', label: 'Overview', key: '01' },
    { id: 'claims', label: 'Warranty claims', key: '02' },
    { id: 'finance', label: 'Vehicle finance', key: '03' },
    { id: 'garage', label: 'My vehicles', key: '04' }
  ];
  section: Section = 'overview';
  readonly maxVehicleYear = new Date().getFullYear() + 1;
  authMode: 'login' | 'register' = 'login';
  user: SessionUser | null = null;
  loading = false;
  busy = false;
  notice = '';
  error = '';
  vehicles: Vehicle[] = [];
  plans: Plan[] = [];
  warranties: Warranty[] = [];
  claims: Claim[] = [];
  applications: FinanceApplication[] = [];
  installments: Installment[] = [];
  credentials = { name: '', email: '', password: '' };
  vehicleForm = { vin: '', make: '', model: '', year: new Date().getFullYear(), currentMileage: 0 };
  warrantyVehicleSelections: Partial<Record<number, string>> = {};
  selectedWarrantyPlanId: number | null = null;
  claimForm = { warrantyId: 0, part: '', description: '', costEstimate: 0 };
  financeForm = { vehicleId: 0, type: 'LOAN', vehiclePrice: 25000, downPayment: 3000, tenureMonths: 48, monthlyIncome: 5000, existingEmi: 0, bureauScore: 720, incomeStable: true };

  async ngOnInit(): Promise<void> {
    if (!localStorage.getItem('ww_token')) return;
    this.loading = true;
    try {
      this.user = await firstValueFrom(this.api.get<SessionUser>('/auth/me'));
      await this.refresh();
    } catch (error) {
      const status = (error as { status?: number } | null)?.status;
      if (status === 401 || status === 403) this.logout();
      else this.error = this.errorMessage(error);
    } finally {
      this.loading = false;
    }
  }

  async authenticate(): Promise<void> {
    this.clearMessages();
    this.busy = true;
    const mode = this.authMode;
    try {
      const endpoint = mode === 'register' ? '/auth/register' : '/auth/login';
      const payload = mode === 'register'
        ? { ...this.credentials, name: this.credentials.name.trim(), email: this.credentials.email.trim() }
        : { ...this.credentials, email: this.credentials.email.trim() };
      const session = await firstValueFrom(this.api.post<SessionUser>(endpoint, payload));
      localStorage.setItem('ww_token', session.token);
      this.user = session;
      await this.refresh();
      this.notice = mode === 'register' ? 'Your account is ready.' : 'Welcome back.';
    } catch (error) {
      this.error = this.errorMessage(error);
    } finally {
      this.busy = false;
    }
  }

  logout(): void {
    localStorage.removeItem('ww_token');
    this.user = null;
    this.vehicles = [];
    this.warranties = [];
    this.claims = [];
    this.applications = [];
    this.installments = [];
    this.section = 'overview';
  }

  async refresh(): Promise<void> {
    const [vehicles, plans, warranties, claims, applications, installments] = await Promise.all([
      firstValueFrom(this.api.get<Vehicle[]>('/vehicles')),
      firstValueFrom(this.api.get<Plan[]>('/warranty/plans')),
      firstValueFrom(this.api.get<Warranty[]>('/warranties')),
      firstValueFrom(this.api.get<Claim[]>('/claims')),
      firstValueFrom(this.api.get<FinanceApplication[]>('/finance/applications')),
      firstValueFrom(this.api.get<Installment[]>('/finance/installments'))
    ]);
    this.vehicles = vehicles;
    this.plans = plans;
    this.warranties = warranties;
    this.claims = claims;
    this.applications = applications;
    this.installments = installments;
    if (!this.financeForm.vehicleId && vehicles.length) this.financeForm.vehicleId = vehicles[0].id;
    if (!this.claimForm.warrantyId && warranties.length) this.claimForm.warrantyId = warranties[0].id;
  }

  async refreshData(): Promise<void> {
    await this.perform(() => this.refresh());
  }

  async addVehicle(): Promise<void> {
    const payload = {
      ...this.vehicleForm,
      vin: this.vehicleForm.vin.trim().toUpperCase(),
      make: this.vehicleForm.make.trim(),
      model: this.vehicleForm.model.trim()
    };
    if (!payload.vin || !payload.make || !payload.model) {
      this.clearMessages();
      this.error = 'Enter a VIN, make, and model to add a vehicle.';
      return;
    }
    await this.perform(async () => {
      await firstValueFrom(this.api.post<Vehicle>('/vehicles', payload));
      this.vehicleForm = { vin: '', make: '', model: '', year: new Date().getFullYear(), currentMileage: 0 };
      await this.refresh();
      this.notice = 'Vehicle added to your garage.';
    });
  }

  async purchaseWarranty(vehicleId: number, planId: number): Promise<void> {
    await this.perform(async () => {
      const warranty = await firstValueFrom(this.api.post<Warranty>('/warranties', { vehicleId, planId }));
      this.warranties = [warranty, ...this.warranties];
      this.warrantyVehicleSelections = {};
      this.selectedWarrantyPlanId = null;
      this.notice = 'Warranty coverage is now active.';
    });
  }

  selectWarrantyPlan(planId: number, vehicleId: string): void {
    this.warrantyVehicleSelections = vehicleId ? { [planId]: vehicleId } : {};
    this.selectedWarrantyPlanId = vehicleId ? planId : null;
  }

  async submitClaim(): Promise<void> {
    await this.perform(async () => {
      await firstValueFrom(this.api.post<Claim>('/claims', this.claimForm));
      await this.refresh();
      this.notice = 'Claim submitted. Eligibility checks are included in its record.';
    });
  }

  async reviewClaim(id: number, approved: boolean): Promise<void> {
    await this.perform(async () => {
      await firstValueFrom(this.api.patch<Claim>(`/claims/${id}/review`, { approved, note: approved ? 'Approved after staff review.' : 'Declined after staff review.' }));
      await this.refresh();
      this.notice = 'Claim review saved.';
    });
  }

  async submitFinance(): Promise<void> {
    await this.perform(async () => {
      await firstValueFrom(this.api.post<FinanceApplication>('/finance/applications', this.financeForm));
      await this.refresh();
      this.notice = 'Finance application scored and saved.';
    });
  }

  async decideFinance(id: number, approved: boolean): Promise<void> {
    await this.perform(async () => {
      await firstValueFrom(this.api.patch<FinanceApplication>(`/finance/applications/${id}/decision`, { approved, note: approved ? 'Approved after credit review.' : 'Declined after credit review.' }));
      await this.refresh();
      this.notice = 'Credit decision saved.';
    });
  }

  async payInstallment(id: number): Promise<void> {
    await this.perform(async () => {
      await firstValueFrom(this.api.post<Installment>(`/finance/installments/${id}/pay`, {}));
      await this.refresh();
      this.notice = 'Installment payment recorded.';
    });
  }

  canReviewClaims(): boolean { return ['ADJUSTER', 'ADMIN'].includes(this.user?.role ?? ''); }
  canReviewFinance(): boolean { return ['CREDIT_OFFICER', 'ADMIN'].includes(this.user?.role ?? ''); }
  openSection(section: Section): void { this.section = section; this.clearMessages(); }
  get pendingClaimCount(): number { return this.claims.filter(claim => ['SUBMITTED', 'UNDER_REVIEW'].includes(claim.status)).length; }
  get nextInstallment(): Installment | undefined { return this.installments.find(item => item.status !== 'PAID'); }
  sectionLabel(sectionId: Section): string | undefined {
    return this.sections.find(item => item.id === sectionId)?.label;
  }

  isVehicleProtected(vehicleId: number): boolean {
    const now = new Date();
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    return this.warranties.some(item => item.vehicleId === vehicleId && item.status === 'ACTIVE'
      && item.startDate <= today && item.endDate >= today);
  }
  get eligibleWarrantyVehicles(): Vehicle[] {
    return this.vehicles.filter(vehicle => !this.isVehicleProtected(vehicle.id));
  }
  statusClass(status: string): string { return status.toLowerCase().replaceAll('_', '-'); }
  errorMessage(error: unknown): string {
    const response = error as { error?: unknown };
    if (typeof response.error === 'string') return response.error;
    if (response.error && typeof response.error === 'object') {
      const body = response.error as { detail?: unknown; message?: unknown; error?: unknown };
      if (typeof body.detail === 'string') return body.detail;
      if (typeof body.message === 'string') return body.message;
      if (typeof body.error === 'string') return body.error;
    }
    return 'Something went wrong. Please try again.';
  }
  clearMessages(): void { this.notice = ''; this.error = ''; }

  private async perform(action: () => Promise<void>): Promise<void> {
    this.clearMessages();
    this.busy = true;
    try { await action(); }
    catch (error) { this.error = this.errorMessage(error); }
    finally { this.busy = false; }
  }
}
