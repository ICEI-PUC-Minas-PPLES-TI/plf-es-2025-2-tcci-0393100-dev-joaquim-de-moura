"use client";

import { FormEvent, ReactNode, useCallback, useEffect, useRef, useState, useSyncExternalStore } from "react";
import type { CellHookData } from "jspdf-autotable";
import dynamic from "next/dynamic";

const LiveMap = dynamic(() => import("./LiveMap"), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center font-bold text-slate-400">
      Carregando mapa...
    </div>
  ),
});

const RegionMap = dynamic(() => import("./RegionMap"), {
  ssr: false,
  loading: () => (
    <div className="flex h-full items-center justify-center font-bold text-slate-400">
      Carregando mapa...
    </div>
  ),
});

const API_URL = (
  process.env.NEXT_PUBLIC_API_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:3000"
).replace(/\/$/, "");
const TOKEN_KEY = "admin_token";
const TOKEN_EVENT = "mobu-admin-token-changed";

type DriverApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
type RideStatus =
  | "PENDING_DRIVER"
  | "ACCEPTED"
  | "DRIVER_ARRIVING"
  | "DRIVER_ARRIVED"
  | "IN_PROGRESS"
  | "FINISHED"
  | "CANCELED";
type PaymentStatus = "PENDING" | "RECEIVED" | "NOT_RECEIVED" | "CANCELED";
type TicketStatus = "OPEN" | "IN_REVIEW" | "RESOLVED" | "CLOSED";

interface UserSummary {
  id: string;
  name: string | null;
  phone: string;
  role?: string;
  blocked?: boolean;
}

interface DriverProfile {
  id: string;
  userId: string;
  online: boolean;
  available: boolean;
  approvalStatus: DriverApprovalStatus;
  rejectionReason: string | null;
  cnhNumber: string | null;
  cnhCategory: string | null;
  cnhExpiresAt: string | null;
  cnhImageUrl: string | null;
  cpf: string | null;
  hasEar: boolean | null;
  vehicleModel: string | null;
  vehiclePlate: string | null;
  vehicleColor: string | null;
  vehicleYear: number | null;
  vehicleCapacity: number | null;
  profilePhotoUrl: string | null;
  pixKey: string | null;
  pixQrPayload: string | null;
  currentLat: number | null;
  currentLng: number | null;
  approvedAt: string | null;
  createdAt: string;
  updatedAt?: string;
  user: UserSummary & { blocked: boolean };
  totalFinishedRides: number;
  averageRating: number | null;
  totalReviews: number;
}

interface Passenger {
  id: string;
  name: string | null;
  phone: string;
  email: string | null;
  blocked: boolean;
  blockedAt: string | null;
  createdAt: string;
  totalRides: number;
  totalTickets: number;
}

interface Ride {
  id: string;
  passengerId: string;
  driverId: string | null;
  status: RideStatus;
  paymentStatus: PaymentStatus;
  paymentMethod?: "CASH" | "PIX";
  originAddress: string | null;
  destinationAddress: string | null;
  originLat: number;
  originLng: number;
  destLat: number;
  destLng: number;
  distanceMeters: number | null;
  durationSeconds: number | null;
  estimatedFareCents: number | null;
  createdAt: string;
  updatedAt: string;
  passenger: UserSummary;
  driver: UserSummary | null;
  review: { rating: number; comment: string | null } | null;
}

interface Stats {
  totalRides: number;
  finishedRides: number;
  canceledRides: number;
  pendingPayments: number;
  receivedPayments: number;
  notReceivedPayments: number;
  openSupportTickets: number;
  onlineDrivers: number;
  availableDrivers: number;
  receivedRevenueCents: number;
}

interface LiveDriver {
  id: string;
  userId: string;
  name: string | null;
  phone: string;
  online: boolean;
  available: boolean;
  currentLat: number | null;
  currentLng: number | null;
  vehicleModel: string | null;
  vehiclePlate: string | null;
  vehicleColor: string | null;
  locationUpdatedAt: string;
}

interface LiveOperation {
  activeRides: Ride[];
  onlineDrivers: LiveDriver[];
  generatedAt: string;
}

interface OperationRegion {
  id: string;
  name: string;
  city: string | null;
  active: boolean;
  centerLat: number | null;
  centerLng: number | null;
  radiusMeters: number | null;
  pricingConfigs?: PricingConfig[];
}

interface PricingConfig {
  id: string;
  name: string | null;
  regionId: string | null;
  isActive: boolean;
  baseFareCents: number;
  perKmCents: number;
  perMinuteCents: number;
  minimumFareCents: number;
  bookingFeeCents: number;
  surgeMultiplier: number;
  currency: string;
  createdAt: string;
  region?: OperationRegion | null;
}

interface ReportData extends Stats {
  estimatedFinishedFareCents: number;
  averageRating: number | null;
  totalReviews: number;
  ridesByStatus: Record<string, number>;
  paymentsByStatus: Record<string, number>;
  driversByApproval: Record<string, number>;
  generatedAt: string;
}

type TicketType = "PAYMENT" | "RIDE_CANCELLATION" | "SAFETY" | "APP_ISSUE" | "OTHER";

interface SupportTicket {
  id: string;
  type: TicketType;
  status: TicketStatus;
  subject: string;
  description: string;
  resolution: string | null;
  createdAt: string;
  updatedAt: string;
  closedAt: string | null;
  creator: UserSummary;
  ride: {
    id: string;
    status: RideStatus;
    paymentStatus: PaymentStatus;
    originAddress: string | null;
    destinationAddress: string | null;
  } | null;
}

interface DriverFinancialRow {
  driverId: string;
  driverName: string | null;
  driverPhone: string | null;
  rides: number;
  grossCents: number;
  feeCents: number;
  receivableCents: number;
  settledCents: number;
  balanceCents: number;
}

interface SettlementRecord {
  id: string;
  driverId: string;
  driverName: string | null;
  amountCents: number;
  notes: string | null;
  method: string;
  settledAt: string;
  settledBy: string;
  paymentRequestId: string | null;
}

interface PaymentRequest {
  id: string;
  driverId: string;
  amountCents: number;
  status: "PENDING" | "CONFIRMED" | "REJECTED";
  notes: string | null;
  receiptUrl: string | null;
  requestedAt: string;
  reviewedAt: string | null;
  rejectionReason: string | null;
  driver: { id: string; name: string | null; phone: string };
  reviewer: { name: string | null } | null;
  settlement: { id: string; amountCents: number } | null;
}

interface SystemConfig {
  DRIVER_DEBT_LIMIT_CENTS?: string;
  PLATFORM_PIX_KEY?: string;
  [key: string]: string | undefined;
}

interface PromoCode {
  id: string;
  code: string;
  discountPercent: number | null;
  discountCents: number | null;
  maxUses: number | null;
  usedCount: number;
  expiresAt: string | null;
  active: boolean;
  createdAt: string;
}

interface FinancialSummary {
  overview: {
    totalRides: number;
    totalGrossCents: number;
    totalPlatformFeeCents: number;
    totalDriverReceivableCents: number;
    totalSettledCents: number;
    totalPendingCents: number;
    pendingPaymentRequests: number;
  };
  byDriver: DriverFinancialRow[];
  recentSettlements: SettlementRecord[];
}

type AuthFetch = <T = unknown>(path: string, init?: RequestInit) => Promise<T>;

function getTokenSnapshot() {
  if (typeof window === "undefined") return null;
  return window.sessionStorage.getItem(TOKEN_KEY);
}

function subscribeToken(onStoreChange: () => void) {
  if (typeof window === "undefined") return () => {};

  window.addEventListener(TOKEN_EVENT, onStoreChange);
  window.addEventListener("storage", onStoreChange);

  return () => {
    window.removeEventListener(TOKEN_EVENT, onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}

function notifyTokenChanged() {
  window.dispatchEvent(new Event(TOKEN_EVENT));
}

function useAuth() {
  const token = useSyncExternalStore(subscribeToken, getTokenSnapshot, () => null);

  const login = async (phone: string, password: string) => {
    const res = await fetch(`${API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ phone, password }),
    });

    if (!res.ok) throw new Error("Credenciais inválidas");

    const data = await res.json();
    if (data.user.role !== "ADMIN") {
      throw new Error("Acesso restrito a administradores");
    }

    sessionStorage.setItem(TOKEN_KEY, data.accessToken);
    notifyTokenChanged();
  };

  const logout = () => {
    sessionStorage.removeItem(TOKEN_KEY);
    notifyTokenChanged();
  };

  return { token, login, logout };
}

function money(cents?: number | null) {
  if (cents == null) return "—";
  return `R$ ${(cents / 100).toFixed(2).replace(".", ",")}`;
}

function dateTime(value?: string | null) {
  if (!value) return "—";
  const d = new Date(value);
  const now = new Date();
  const sameYear = d.getFullYear() === now.getFullYear();
  return d.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    ...(sameYear ? {} : { year: "numeric" }),
    hour: "2-digit",
    minute: "2-digit",
  });
}

function km(meters?: number | null) {
  if (meters == null) return null;
  return `${(meters / 1000).toFixed(1).replace(".", ",")} km`;
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    PENDING: "Pendente",
    APPROVED: "Aprovado",
    REJECTED: "Rejeitado",
    PENDING_DRIVER: "Procurando",
    ACCEPTED: "Aceita",
    DRIVER_ARRIVING: "A caminho",
    DRIVER_ARRIVED: "Chegou",
    IN_PROGRESS: "Em andamento",
    FINISHED: "Finalizada",
    CANCELED: "Cancelada",
    RECEIVED: "Recebido",
    NOT_RECEIVED: "Não recebido",
    OPEN: "Aberto",
    IN_REVIEW: "Em análise",
    RESOLVED: "Resolvido",
    CLOSED: "Fechado",
  };
  return labels[value] ?? value;
}

function classNames(...items: Array<string | false | null | undefined>) {
  return items.filter(Boolean).join(" ");
}

async function downloadFile(url: string, filename: string) {
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error("Falha ao buscar arquivo");
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = href;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(href);
  } catch {
    // fallback: open in new tab
    window.open(url, "_blank");
  }
}

type ReportPdfOptions = {
  r: ReportData;
  from: string;
  to: string;
};

type AutoTableDocument = {
  lastAutoTable?: {
    finalY?: number;
  };
};

async function generateReportPdf({ r, from, to }: ReportPdfOptions) {
  const { default: jsPDF } = await import("jspdf");
  const { default: autoTable } = await import("jspdf-autotable");

  const doc = new jsPDF({ orientation: "portrait", unit: "mm", format: "a4" });
  const pageW = doc.internal.pageSize.getWidth();
  const pageH = doc.internal.pageSize.getHeight();
  const margin = 14;
  let y = 0;
  const lastTableY = () => (doc as unknown as AutoTableDocument).lastAutoTable?.finalY ?? y;

  const VIOLET = [109, 40, 217] as [number, number, number];
  const VIOLET_LIGHT = [237, 233, 254] as [number, number, number];
  const SLATE_900 = [15, 23, 42] as [number, number, number];
  const SLATE_500 = [100, 116, 139] as [number, number, number];
  const SLATE_200 = [226, 232, 240] as [number, number, number];
  const EMERALD = [16, 185, 129] as [number, number, number];
  const AMBER = [245, 158, 11] as [number, number, number];
  const RED = [239, 68, 68] as [number, number, number];
  const BLUE = [59, 130, 246] as [number, number, number];
  const WHITE = [255, 255, 255] as [number, number, number];

  // ── Header ────────────────────────────────────────────────────────────────
  doc.setFillColor(...VIOLET);
  doc.rect(0, 0, pageW, 30, "F");
  doc.setTextColor(...WHITE);
  doc.setFontSize(18);
  doc.setFont("helvetica", "bold");
  doc.text("MobU — Relatório Operacional", margin, 13);
  doc.setFontSize(8);
  doc.setFont("helvetica", "normal");
  const periodLabel =
    from || to
      ? `Período: ${from || "início"} → ${to || "hoje"}`
      : "Período: todos os registros";
  doc.text(periodLabel, margin, 20);
  doc.text(
    `Gerado em: ${new Date().toLocaleString("pt-BR")}`,
    pageW - margin,
    20,
    { align: "right" }
  );
  y = 38;

  const moneyFmt = (cents?: number | null) =>
    cents == null ? "—" : `R$ ${(cents / 100).toFixed(2).replace(".", ",")}`;

  const pctFmt = (n: number, d: number) =>
    d === 0 ? "0%" : `${Math.round((n / d) * 100)}%`;

  function sectionTitle(title: string) {
    doc.setFillColor(...VIOLET_LIGHT);
    doc.rect(margin - 2, y - 4, pageW - margin * 2 + 4, 7, "F");
    doc.setTextColor(...VIOLET);
    doc.setFontSize(8);
    doc.setFont("helvetica", "bold");
    doc.text(title.toUpperCase(), margin, y);
    y += 4;
  }

  function checkPageBreak(needed = 20) {
    if (y + needed > pageH - 16) {
      doc.addPage();
      y = 16;
    }
  }

  // ── KPIs ──────────────────────────────────────────────────────────────────
  sectionTitle("Destaques");
  y += 2;

  const totalRides = r.totalRides ?? 0;
  const finished = r.finishedRides ?? 0;
  const canceled = r.canceledRides ?? 0;
  const received = r.receivedPayments ?? 0;
  const notReceived = r.notReceivedPayments ?? 0;
  const pending = r.pendingPayments ?? 0;
  const totalPayments = received + notReceived + pending;
  const completionRate = totalRides ? Math.round((finished / totalRides) * 100) : 0;
  const avgTicket =
    received > 0 && r.receivedRevenueCents
      ? moneyFmt(Math.round(r.receivedRevenueCents / received))
      : "—";
  const approved = r.driversByApproval?.APPROVED ?? 0;
  const pendingDrivers = r.driversByApproval?.PENDING ?? 0;
  const rejectedDrivers = r.driversByApproval?.REJECTED ?? 0;
  const totalDrivers = approved + pendingDrivers + rejectedDrivers;
  const online = r.onlineDrivers ?? 0;
  const available = Math.min(r.availableDrivers ?? 0, online);
  const busy = Math.max(0, online - available);
  const revenueGap =
    r.estimatedFinishedFareCents != null && r.receivedRevenueCents != null
      ? r.estimatedFinishedFareCents - r.receivedRevenueCents
      : null;

  autoTable(doc, {
    startY: y,
    margin: { left: margin, right: margin },
    head: [["Métrica", "Valor", "Detalhe"]],
    body: [
      ["Receita recebida", moneyFmt(r.receivedRevenueCents), `de ${moneyFmt(r.estimatedFinishedFareCents)} estimado`],
      ["Taxa de conclusão", `${completionRate}%`, `${finished} de ${totalRides} corridas`],
      ["Ticket médio", avgTicket, `${received} pagamentos recebidos`],
      ["Avaliação média", r.averageRating != null ? `${r.averageRating.toFixed(1).replace(".", ",")} ★` : "—", `${r.totalReviews ?? 0} avaliações`],
      ...(revenueGap != null && revenueGap > 0 ? [["Gap de receita", moneyFmt(revenueGap), "Diferença entre estimado e recebido"]] : []),
    ],
    headStyles: { fillColor: VIOLET, textColor: WHITE, fontStyle: "bold", fontSize: 8 },
    bodyStyles: { fontSize: 8, textColor: SLATE_900 },
    alternateRowStyles: { fillColor: [248, 250, 252] as [number, number, number] },
    columnStyles: { 1: { fontStyle: "bold" } },
  });
  y = lastTableY() + 8;

  // ── Corridas ──────────────────────────────────────────────────────────────
  checkPageBreak(40);
  sectionTitle("Corridas por status");
  y += 2;

  const inProgress =
    (r.ridesByStatus?.IN_PROGRESS ?? 0) +
    (r.ridesByStatus?.ACCEPTED ?? 0) +
    (r.ridesByStatus?.DRIVER_ARRIVING ?? 0) +
    (r.ridesByStatus?.DRIVER_ARRIVED ?? 0) +
    (r.ridesByStatus?.PENDING_DRIVER ?? 0);

  autoTable(doc, {
    startY: y,
    margin: { left: margin, right: margin },
    head: [["Status", "Quantidade", "% do total"]],
    body: [
      ["Finalizadas", finished, pctFmt(finished, totalRides)],
      ["Em andamento", inProgress, pctFmt(inProgress, totalRides)],
      ["Canceladas", canceled, pctFmt(canceled, totalRides)],
      ["Total", totalRides, "100%"],
    ],
    headStyles: { fillColor: EMERALD, textColor: WHITE, fontStyle: "bold", fontSize: 8 },
    bodyStyles: { fontSize: 8, textColor: SLATE_900 },
    alternateRowStyles: { fillColor: [240, 253, 244] as [number, number, number] },
    didParseCell: (data: CellHookData) => {
      if (data.row.index === 3) {
        data.cell.styles.fontStyle = "bold";
        data.cell.styles.fillColor = SLATE_200;
      }
      if (data.column.index === 0 && data.row.index === 2 && canceled > 0 && totalRides > 0 && canceled / totalRides > 0.2) {
        data.cell.styles.textColor = RED;
      }
    },
  });
  y = lastTableY() + 8;

  // ── Pagamentos ────────────────────────────────────────────────────────────
  checkPageBreak(40);
  sectionTitle("Pagamentos");
  y += 2;

  autoTable(doc, {
    startY: y,
    margin: { left: margin, right: margin },
    head: [["Status", "Quantidade", "% do total"]],
    body: [
      ["Recebidos", received, pctFmt(received, totalPayments)],
      ["Pendentes", pending, pctFmt(pending, totalPayments)],
      ["Não recebidos", notReceived, pctFmt(notReceived, totalPayments)],
      ["Taxa de recebimento", `${pctFmt(received, totalPayments)}`, ""],
    ],
    headStyles: { fillColor: BLUE, textColor: WHITE, fontStyle: "bold", fontSize: 8 },
    bodyStyles: { fontSize: 8, textColor: SLATE_900 },
    alternateRowStyles: { fillColor: [239, 246, 255] as [number, number, number] },
    didParseCell: (data: CellHookData) => {
      if (data.row.index === 3) {
        data.cell.styles.fontStyle = "bold";
        data.cell.styles.fillColor = SLATE_200;
      }
      if (data.column.index === 0 && data.row.index === 2 && notReceived > 0) {
        data.cell.styles.textColor = RED;
      }
    },
  });
  y = lastTableY() + 8;

  // ── Motoristas ────────────────────────────────────────────────────────────
  checkPageBreak(50);
  sectionTitle("Base de motoristas");
  y += 2;

  autoTable(doc, {
    startY: y,
    margin: { left: margin, right: margin },
    head: [["Status", "Quantidade", "% do total"]],
    body: [
      ["Aprovados", approved, pctFmt(approved, totalDrivers)],
      ["Pendentes de aprovação", pendingDrivers, pctFmt(pendingDrivers, totalDrivers)],
      ["Rejeitados", rejectedDrivers, pctFmt(rejectedDrivers, totalDrivers)],
      ["Total cadastrados", totalDrivers, "100%"],
    ],
    headStyles: { fillColor: VIOLET, textColor: WHITE, fontStyle: "bold", fontSize: 8 },
    bodyStyles: { fontSize: 8, textColor: SLATE_900 },
    alternateRowStyles: { fillColor: [245, 243, 255] as [number, number, number] },
    didParseCell: (data: CellHookData) => {
      if (data.row.index === 3) {
        data.cell.styles.fontStyle = "bold";
        data.cell.styles.fillColor = SLATE_200;
      }
      if (data.column.index === 0 && data.row.index === 1 && pendingDrivers > 0) {
        data.cell.styles.textColor = AMBER;
      }
    },
  });
  y = lastTableY() + 8;

  // ── Operação em tempo real ────────────────────────────────────────────────
  checkPageBreak(40);
  sectionTitle("Operação em tempo real");
  y += 2;

  autoTable(doc, {
    startY: y,
    margin: { left: margin, right: margin },
    head: [["Métrica", "Valor"]],
    body: [
      ["Motoristas online", online],
      ["Disponíveis", available],
      ["Em corrida (ocupados)", busy],
      ["Taxa de ocupação", pctFmt(busy, online || 1)],
      ["Chamados abertos", r.openSupportTickets ?? 0],
    ],
    headStyles: { fillColor: SLATE_900, textColor: WHITE, fontStyle: "bold", fontSize: 8 },
    bodyStyles: { fontSize: 8, textColor: SLATE_900 },
    alternateRowStyles: { fillColor: [248, 250, 252] as [number, number, number] },
    didParseCell: (data: CellHookData) => {
      if (data.column.index === 0 && data.row.index === 4 && (r.openSupportTickets ?? 0) > 0) {
        data.cell.styles.textColor = AMBER;
        data.cell.styles.fontStyle = "bold";
      }
    },
  });
  y = lastTableY() + 8;

  // ── Footer ────────────────────────────────────────────────────────────────
  const totalPages = doc.getNumberOfPages();
  for (let i = 1; i <= totalPages; i++) {
    doc.setPage(i);
    doc.setFillColor(...SLATE_200);
    doc.rect(0, pageH - 10, pageW, 10, "F");
    doc.setTextColor(...SLATE_500);
    doc.setFontSize(7);
    doc.setFont("helvetica", "normal");
    doc.text("MobU Admin — Documento gerado automaticamente. Uso interno.", margin, pageH - 4);
    doc.text(`Página ${i} de ${totalPages}`, pageW - margin, pageH - 4, { align: "right" });
  }

  const dateStr = new Date().toISOString().slice(0, 10);
  const periodStr = from && to ? `_${from}_a_${to}` : from ? `_de_${from}` : to ? `_ate_${to}` : "";
  doc.save(`MobU_Relatorio${periodStr}_${dateStr}.pdf`);
}


async function parseError(res: Response) {
  try {
    const body = await res.json();
    return Array.isArray(body.message) ? body.message.join(", ") : body.message ?? res.statusText;
  } catch {
    return res.statusText;
  }
}

function LoginForm({ onLogin }: { onLogin: (phone: string, password: string) => Promise<void> }) {
  const [phone, setPhone] = useState("31999000000");
  const [password, setPassword] = useState("Senha@123");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await onLogin(phone, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao entrar");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#F5F3FF] text-slate-950">
      <div className="mx-auto flex min-h-screen max-w-6xl items-center justify-center px-6">
        <section className="grid w-full overflow-hidden rounded-[2rem] bg-white shadow-2xl md:grid-cols-[1.05fr_0.95fr]">
          <div className="bg-slate-950 p-10 text-white">
            <p className="text-sm font-bold uppercase tracking-[0.28em] text-violet-300">MobU Admin</p>
            <h1 className="mt-6 text-4xl font-black leading-tight">Operação, motoristas e tarifas em um só painel.</h1>
            <p className="mt-4 max-w-md text-slate-300">
              Acompanhe corridas, aprove motoristas, configure regiões e resolva chamados sem depender do banco.
            </p>
            <div className="mt-10 grid grid-cols-2 gap-3 text-sm">
              {["Corridas ao vivo", "Tarifas por região", "Suporte", "Pagamentos"].map((item) => (
                <div key={item} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  {item}
                </div>
              ))}
            </div>
          </div>

          <form onSubmit={submit} className="p-8 md:p-10">
            <h2 className="text-2xl font-black">Entrar</h2>
            <p className="mt-1 text-sm text-slate-500">Use o login administrativo de teste.</p>
            <label className="mt-8 block text-sm font-bold text-slate-700">Telefone</label>
            <input
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-violet-600"
              placeholder="31999999999"
            />
            <label className="mt-5 block text-sm font-bold text-slate-700">Senha</label>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-violet-600"
              placeholder="admin123"
            />
            {error && <p className="mt-4 rounded-2xl bg-red-50 p-3 text-sm font-semibold text-red-700">{error}</p>}
            <button
              disabled={loading}
              className="mt-6 w-full rounded-2xl bg-violet-700 px-5 py-3 font-black text-white transition hover:bg-violet-800 disabled:opacity-60"
            >
              {loading ? "Entrando..." : "Acessar painel"}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}

function Badge({ value, tone = "neutral" }: { value: string; tone?: "neutral" | "green" | "yellow" | "red" | "blue" }) {
  const tones = {
    neutral: "bg-slate-100 text-slate-700",
    green: "bg-emerald-100 text-emerald-800",
    yellow: "bg-amber-100 text-amber-800",
    red: "bg-red-100 text-red-700",
    blue: "bg-sky-100 text-sky-800",
  };
  return <span className={classNames("rounded-full px-3 py-1 text-xs font-black", tones[tone])}>{value}</span>;
}

function statusTone(value: string): "neutral" | "green" | "yellow" | "red" | "blue" {
  if (["APPROVED", "FINISHED", "RECEIVED", "RESOLVED", "CLOSED"].includes(value)) return "green";
  if (["PENDING", "PENDING_DRIVER", "OPEN", "IN_REVIEW"].includes(value)) return "yellow";
  if (["REJECTED", "CANCELED", "NOT_RECEIVED"].includes(value)) return "red";
  if (["ACCEPTED", "DRIVER_ARRIVING", "DRIVER_ARRIVED", "IN_PROGRESS"].includes(value)) return "blue";
  return "neutral";
}

function MetricCard({ label, value, hint }: { label: string; value: string | number; hint?: string }) {
  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm font-bold text-slate-500">{label}</p>
      <p className="mt-4 text-3xl font-black text-slate-950">{value}</p>
      {hint && <p className="mt-2 text-xs font-semibold text-slate-400">{hint}</p>}
    </div>
  );
}

function SectionCard({
  title,
  subtitle,
  action,
  children,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <section className="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-black text-slate-950">{title}</h2>
          {subtitle && <p className="mt-1 text-sm font-medium text-slate-500">{subtitle}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}

export default function AdminDashboard() {
  const { token, login, logout } = useAuth();
  const [stats, setStats] = useState<Stats | null>(null);
  const [reports, setReports] = useState<ReportData | null>(null);
  const [live, setLive] = useState<LiveOperation | null>(null);
  const [rides, setRides] = useState<Ride[]>([]);
  const [drivers, setDrivers] = useState<DriverProfile[]>([]);
  const [passengers, setPassengers] = useState<Passenger[]>([]);
  const [pricing, setPricing] = useState<PricingConfig[]>([]);
  const [regions, setRegions] = useState<OperationRegion[]>([]);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [financial, setFinancial] = useState<FinancialSummary | null>(null);
  const [paymentRequests, setPaymentRequests] = useState<PaymentRequest[]>([]);
  const [systemConfig, setSystemConfig] = useState<SystemConfig>({});
  const [promoCodes, setPromoCodes] = useState<PromoCode[]>([]);
  const [driverFilter, setDriverFilter] = useState<"PENDING" | "APPROVED" | "REJECTED" | "ALL">("PENDING");
  const [rideStatus, setRideStatus] = useState("");
  const [paymentStatus, setPaymentStatus] = useState("");
  const [reportFrom, setReportFrom] = useState("");
  const [reportTo, setReportTo] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const [activeSection, setActiveSection] = useState("");

  const navClickLockRef = useRef(false);

  const handleNavClick = useCallback((id: string) => {
    setActiveSection(id);
    navClickLockRef.current = true;
    setTimeout(() => { navClickLockRef.current = false; }, 900);
  }, []);

  // Ref that always points to the latest loadAll closure.
  // This lets the setInterval call the current version without being recreated.
  const loadAllRef = useRef<((silent?: boolean) => Promise<void>) | null>(null);

  // Tracks in-flight requests: if a new fetch starts before the previous one
  // finishes, we ignore the stale response.
  const fetchIdRef = useRef(0);

  const authFetch = async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
    if (!token) throw new Error("Sessão inválida");
    const headers = new Headers(init.headers);
    headers.set("Authorization", `Bearer ${token}`);
    if (init.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    const res = await fetch(`${API_URL}${path}`, { ...init, headers });
    if (res.status === 401 || res.status === 403) {
      logout();
      throw new Error("Sessão expirada");
    }
    if (!res.ok) throw new Error(await parseError(res));
    return (res.status === 204 ? null : await res.json()) as T;
  };

  const loadAll = async (silent = false) => {
    if (!token) return;

    const myId = ++fetchIdRef.current;
    const MIN_SPIN_MS = 700;
    const started = Date.now();

    if (!silent) setLoading(true);
    setError("");
    try {
      const rideQuery = new URLSearchParams();
      if (rideStatus) rideQuery.set("status", rideStatus);
      if (paymentStatus) rideQuery.set("paymentStatus", paymentStatus);

      const reportQuery = new URLSearchParams();
      if (reportFrom) reportQuery.set("from", reportFrom);
      if (reportTo) reportQuery.set("to", reportTo);

      const driverEndpoint =
        driverFilter === "ALL" ? "/admin/drivers/all" : `/admin/drivers/${driverFilter.toLowerCase()}`;

      const [statsData, reportsData, liveData, ridesData, driversData, passengersData, pricingData, regionsData, ticketsData, financialData, paymentRequestsData, configData, promoCodesData] =
        await Promise.all([
          authFetch<Stats>("/admin/stats"),
          authFetch<ReportData>(`/admin/reports${reportQuery.toString() ? `?${reportQuery.toString()}` : ""}`),
          authFetch<LiveOperation>("/admin/live"),
          authFetch<Ride[]>(`/admin/rides${rideQuery.toString() ? `?${rideQuery.toString()}` : ""}`),
          authFetch<DriverProfile[]>(driverEndpoint),
          authFetch<Passenger[]>("/admin/passengers"),
          authFetch<PricingConfig[]>("/admin/pricing"),
          authFetch<OperationRegion[]>("/admin/regions"),
          authFetch<SupportTicket[]>("/support/tickets"),
          authFetch<FinancialSummary>("/admin/financial"),
          authFetch<PaymentRequest[]>("/admin/financial/payment-requests"),
          authFetch<SystemConfig>("/admin/config"),
          authFetch<PromoCode[]>("/admin/promo-codes"),
        ]);

      // Discard if a newer fetch already completed
      if (myId !== fetchIdRef.current) return;

      setStats(statsData);
      setReports(reportsData);
      setLive(liveData);
      setRides(ridesData);
      setDrivers(driversData);
      setPassengers(passengersData);
      setPricing(pricingData);
      setRegions(regionsData);
      setTickets(ticketsData);
      setFinancial(financialData);
      setPaymentRequests(paymentRequestsData);
      setSystemConfig(configData);
      setPromoCodes(promoCodesData);
    } catch (err) {
      if (myId !== fetchIdRef.current) return;
      setError(err instanceof Error ? err.message : "Erro ao carregar painel");
    } finally {
      if (myId === fetchIdRef.current && !silent) {
        const elapsed = Date.now() - started;
        if (elapsed < MIN_SPIN_MS) {
          await new Promise((r) => setTimeout(r, MIN_SPIN_MS - elapsed));
        }
        if (myId === fetchIdRef.current) setLoading(false);
      }
    }
  };

  // Keep the ref pointing to the latest closure on every render.
  loadAllRef.current = loadAll;

  // User-triggered refresh: shows the spinning animation.
  const triggerLoadAll = useCallback(() => {
    loadAllRef.current?.();
  }, []);

  // Background auto-refresh: updates data silently, no spinner.
  const triggerSilent = useCallback(() => {
    loadAllRef.current?.(true);
  }, []);

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, driverFilter, rideStatus, paymentStatus]);

  useEffect(() => {
    if (!token) return;
    const id = window.setInterval(() => {
      triggerSilent();
    }, 8000);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  useEffect(() => {
    if (!token) return;
    const ids = NAV_LINKS.map(l => l.href.slice(1));
    const handle = () => {
      if (navClickLockRef.current) return;
      const y = window.scrollY + 100;
      let cur = ids[0];
      for (const id of ids) {
        const el = document.getElementById(id);
        if (el && el.offsetTop <= y) cur = id;
      }
      setActiveSection(cur);
    };
    handle();
    window.addEventListener("scroll", handle, { passive: true });
    return () => window.removeEventListener("scroll", handle);
  }, [token]);

  if (!token) return <LoginForm onLogin={login} />;

  return (
    <main className="min-h-screen bg-[#F5F3FF] text-slate-950">
      <header className="sticky top-0 z-20 bg-violet-500 shadow-md">
        <div className="mx-auto flex max-w-7xl items-center gap-4 px-6 py-3">
          {/* Brand */}
          <div className="flex shrink-0 items-center gap-3">
            <div className="leading-tight">
              <p className="text-[10px] font-bold uppercase tracking-widest text-violet-100">Painel Admin</p>
              <p className="text-base font-black text-white">MobU</p>
            </div>
          </div>

          {/* Nav desktop */}
          <div className="min-w-0 flex-1">
            <QuickNav
              pendingPayments={financial?.overview.pendingPaymentRequests ?? paymentRequests.filter(r => r.status === "PENDING").length}
              activeSection={activeSection}
              onNavigate={handleNavClick}
            />
          </div>

          {/* Ações */}
          <div className="flex shrink-0 items-center gap-2">
            {/* Hamburger — apenas mobile */}
            <button
              onClick={() => setMobileNavOpen(o => !o)}
              className="flex items-center justify-center rounded-xl bg-white/15 p-2 ring-1 ring-white/20 transition hover:bg-white/25 md:hidden"
              aria-label="Menu"
            >
              {mobileNavOpen ? (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              ) : (
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              )}
            </button>
            <button
              onClick={triggerLoadAll}
              disabled={loading}
              className="flex items-center gap-2 rounded-xl bg-white/15 px-4 py-2 text-sm font-bold text-white ring-1 ring-white/20 transition hover:bg-white/25 disabled:opacity-60"
            >
              <svg
                width="14" height="14" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
                className={loading ? "animate-spin" : ""}
              >
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
              <span className="hidden sm:inline">Atualizar</span>
            </button>
            <button onClick={logout} className="rounded-xl bg-white/10 px-4 py-2 text-sm font-bold text-white/80 ring-1 ring-white/20 transition hover:bg-white/20 hover:text-white">
              Sair
            </button>
          </div>
        </div>

        {/* Nav mobile dropdown */}
        {mobileNavOpen && (
          <div className="border-t border-white/20 bg-violet-600 px-6 pb-3 md:hidden">
            <nav className="flex flex-col gap-0.5 pt-2">
              {NAV_LINKS.map(link => {
                const isActive = activeSection === link.href.slice(1);
                return (
                  <a
                    key={link.href}
                    href={link.href}
                    onClick={() => { setMobileNavOpen(false); handleNavClick(link.href.slice(1)); }}
                    className={`relative rounded-xl px-4 py-2.5 text-sm font-bold transition ${isActive ? "bg-white/25 text-white" : "text-white/75 hover:bg-white/15 hover:text-white"}`}
                  >
                    {link.label}
                    {link.href === "#cobrancas" && (financial?.overview.pendingPaymentRequests ?? 0) > 0 && (
                      <span className="ml-2 inline-flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-black text-white">
                        {(financial?.overview.pendingPaymentRequests ?? 0) > 9 ? "9+" : financial?.overview.pendingPaymentRequests}
                      </span>
                    )}
                  </a>
                );
              })}
            </nav>
          </div>
        )}

        {loading && <div className="h-0.5 w-full bg-white/40 [animation:progress_0.7s_ease-in-out]" />}
      </header>

      <div className="mx-auto grid max-w-7xl gap-5 px-6 py-6">
        {error && (
          <div className="rounded-3xl border border-red-200 bg-red-50 p-4 text-sm font-bold text-red-700">{error}</div>
        )}

        <div id="visao-geral" className="scroll-mt-24">
          <Overview stats={stats} />
        </div>

        <div id="operacao-ao-vivo" className="scroll-mt-24">
          <LiveOperationPanel live={live} />
        </div>

        <div id="tarifas" className="scroll-mt-24">
          <TariffsPanel
            pricing={pricing}
            regions={regions}
            authFetch={authFetch}
            onChanged={triggerLoadAll}
          />
        </div>

        <div id="cupons" className="scroll-mt-24">
          <CuponsPanel
            promoCodes={promoCodes}
            authFetch={authFetch}
            onChanged={triggerLoadAll}
          />
        </div>

        <div id="corridas" className="scroll-mt-24">
          <RidesPanel
            rides={rides}
            rideStatus={rideStatus}
            paymentStatus={paymentStatus}
            onRideStatus={setRideStatus}
            onPaymentStatus={setPaymentStatus}
          />
        </div>

        <div id="relatorios" className="scroll-mt-24">
          <ReportsPanel
            reports={reports}
            reportFrom={reportFrom}
            reportTo={reportTo}
            onFromChange={setReportFrom}
            onToChange={setReportTo}
            onFilter={triggerLoadAll}
          />
        </div>

        <div id="motoristas" className="scroll-mt-24">
          <DriversPanel
            drivers={drivers}
            filter={driverFilter}
            onFilter={setDriverFilter}
            authFetch={authFetch}
            onChanged={triggerLoadAll}
          />
        </div>

        <div id="passageiros" className="scroll-mt-24">
          <PassengersPanel passengers={passengers} authFetch={authFetch} onChanged={triggerLoadAll} />
        </div>

        <div id="suporte" className="scroll-mt-24">
          <SupportPanel tickets={tickets} authFetch={authFetch} onChanged={triggerLoadAll} />
        </div>

        <div id="cobrancas" className="scroll-mt-24">
          <FinancialPanel
            financial={financial}
            paymentRequests={paymentRequests}
            systemConfig={systemConfig}
            authFetch={authFetch}
            onChanged={triggerLoadAll}
          />
        </div>
      </div>
    </main>
  );
}

const NAV_LINKS: { label: string; href: string }[] = [
  { label: "Visão Geral", href: "#visao-geral" },
  { label: "Operação ao Vivo", href: "#operacao-ao-vivo" },
  { label: "Tarifas", href: "#tarifas" },
  { label: "Cupons", href: "#cupons" },
  { label: "Corridas", href: "#corridas" },
  { label: "Relatórios", href: "#relatorios" },
  { label: "Motoristas", href: "#motoristas" },
  { label: "Passageiros", href: "#passageiros" },
  { label: "Suporte", href: "#suporte" },
  { label: "Cobranças", href: "#cobrancas" },
];

function QuickNav({ pendingPayments = 0, activeSection = "", onNavigate }: { pendingPayments?: number; activeSection?: string; onNavigate?: (id: string) => void }) {
  return (
    <nav className="hidden items-center gap-0.5 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden md:flex">
      {NAV_LINKS.map((link) => {
        const isActive = activeSection === link.href.slice(1);
        return (
          <a
            key={link.href}
            href={link.href}
            onClick={() => onNavigate?.(link.href.slice(1))}
            className={`relative shrink-0 rounded-xl px-3 py-1.5 text-xs font-bold transition ${isActive ? "bg-white/25 text-white" : "text-white/70 hover:bg-white/15 hover:text-white"}`}
          >
            {link.label}
            {link.href === "#cobrancas" && pendingPayments > 0 && (
              <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-black text-white">
                {pendingPayments > 9 ? "9+" : pendingPayments}
              </span>
            )}
          </a>
        );
      })}
    </nav>
  );
}

function Overview({ stats }: { stats: Stats | null }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      <MetricCard label="Total de corridas" value={stats?.totalRides ?? "-"} />
      <MetricCard label="Finalizadas" value={stats?.finishedRides ?? "-"} />
      <MetricCard label="Receita recebida" value={money(stats?.receivedRevenueCents)} />
      <MetricCard label="Pagamentos pendentes" value={stats?.pendingPayments ?? "-"} />
      <MetricCard label="Chamados abertos" value={stats?.openSupportTickets ?? "-"} />
    </div>
  );
}

function LiveOperationPanel({ live }: { live: LiveOperation | null }) {
  const [sideTab, setSideTab] = useState<"rides" | "drivers">("rides");
  const rides = live?.activeRides ?? [];
  const drivers = live?.onlineDrivers ?? [];

  // "Ocupados" vem das corridas ativas com motorista atribuído — fonte confiável
  // independe de o motorista estar ou não na lista onlineDrivers
  const ridesWithDriver = rides.filter((r) => r.driverId != null).length;
  const ridesPending = rides.filter((r) => r.driverId == null).length;
  const availableDrivers = drivers.filter((d) => d.available).length;

  return (
    <SectionCard
      title="Operação ao vivo"
      subtitle="Posições em tempo real atualizadas a cada 8 segundos."
      action={
        <div className="flex items-center gap-3">
          {(rides.length > 0 || drivers.length > 0) && (
            <span className="flex items-center gap-1.5 text-sm font-bold text-violet-700">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-violet-500 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-violet-600" />
              </span>
              Ao vivo
            </span>
          )}
          <Badge value={live ? dateTime(live.generatedAt) : "Aguardando"} tone="blue" />
        </div>
      }
    >
      <div className="grid gap-5 lg:grid-cols-[1.5fr_0.85fr]">
        {/* Mapa */}
        <div className="relative z-0 h-[480px] overflow-hidden rounded-3xl border border-slate-200 [&_.leaflet-container]:rounded-3xl">
          <LiveMap drivers={drivers} rides={rides} />
        </div>

        {/* Painel lateral */}
        <div className="flex flex-col gap-0 overflow-hidden rounded-3xl border border-slate-200 bg-white">
          {/* Metricas resumidas */}
          <div className="grid grid-cols-4 divide-x divide-slate-100 border-b border-slate-100">
            <div className="p-3 text-center">
              <p className="text-2xl font-black text-slate-900">{ridesWithDriver}</p>
              <p className="mt-0.5 text-xs font-bold text-slate-400">Em corrida</p>
            </div>
            <div className="p-3 text-center">
              <p className="text-2xl font-black text-amber-500">{ridesPending}</p>
              <p className="mt-0.5 text-xs font-bold text-slate-400">Aguardando</p>
            </div>
            <div className="p-3 text-center">
              <p className="text-2xl font-black text-violet-700">{availableDrivers}</p>
              <p className="mt-0.5 text-xs font-bold text-slate-400">Livres</p>
            </div>
            <div className="p-3 text-center">
              <p className="text-2xl font-black text-slate-500">{drivers.length}</p>
              <p className="mt-0.5 text-xs font-bold text-slate-400">Online</p>
            </div>
          </div>

          {/* Tabs */}
          <div className="flex border-b border-slate-100">
            {([
              { key: "rides", label: "Corridas", count: rides.length },
              { key: "drivers", label: "Motoristas", count: drivers.length },
            ] as const).map(({ key, label, count }) => (
              <button
                key={key}
                onClick={() => setSideTab(key)}
                className={classNames(
                  "flex flex-1 items-center justify-center gap-2 py-3 text-sm font-black transition-colors",
                  sideTab === key
                    ? "border-b-2 border-violet-600 text-violet-700"
                    : "text-slate-400 hover:text-slate-600"
                )}
              >
                {label}
                {count > 0 && (
                  <span className={classNames(
                    "rounded-full px-2 py-0.5 text-xs",
                    sideTab === key ? "bg-violet-100 text-violet-700" : "bg-slate-100 text-slate-500"
                  )}>{count}</span>
                )}
              </button>
            ))}
          </div>

          {/* Conteudo da tab */}
          <div className="flex-1 overflow-y-auto">
            {sideTab === "rides" && (
              rides.length === 0 ? (
                <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
                  <p className="text-3xl">🚗</p>
                  <p className="mt-2 font-bold text-slate-400">Nenhuma corrida ativa agora</p>
                </div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {rides.map((ride) => (
                    <div key={ride.id} className="px-4 py-3.5 hover:bg-slate-50">
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-black text-slate-900 leading-tight">{ride.passenger?.name ?? "Passageiro"}</p>
                        <Badge value={statusLabel(ride.status)} tone={statusTone(ride.status)} />
                      </div>
                      {(ride.originAddress || ride.destinationAddress) && (
                        <div className="mt-1.5 grid gap-0.5">
                          {ride.originAddress && (
                            <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-500">
                              <span className="h-1.5 w-1.5 flex-shrink-0 rounded-full bg-violet-500" />
                              <span className="truncate">{ride.originAddress}</span>
                            </p>
                          )}
                          {ride.destinationAddress && (
                            <p className="flex items-center gap-1.5 text-xs font-semibold text-slate-400">
                              <span className="h-1.5 w-1.5 flex-shrink-0 rounded-full bg-red-400" />
                              <span className="truncate">{ride.destinationAddress}</span>
                            </p>
                          )}
                        </div>
                      )}
                      <div className="mt-1.5 flex items-center gap-3 text-xs font-semibold text-slate-400">
                        <span>{money(ride.estimatedFareCents)}</span>
                        {ride.distanceMeters && <span>{km(ride.distanceMeters)}</span>}
                        {ride.driver && <span className="text-slate-500">{ride.driver.name}</span>}
                      </div>
                    </div>
                  ))}
                </div>
              )
            )}

            {sideTab === "drivers" && (
              drivers.length === 0 ? (
                <div className="flex flex-col items-center justify-center px-6 py-12 text-center">
                  <p className="text-3xl">🏎️</p>
                  <p className="mt-2 font-bold text-slate-400">Nenhum motorista online agora</p>
                </div>
              ) : (
                <div className="divide-y divide-slate-50">
                  {drivers.map((driver) => (
                    <div key={driver.id} className="flex items-center gap-3 px-4 py-3.5 hover:bg-slate-50">
                      <div className={classNames(
                        "h-2.5 w-2.5 flex-shrink-0 rounded-full",
                        driver.available ? "bg-violet-500" : "bg-amber-400"
                      )} />
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-black text-slate-900">{driver.name ?? "Motorista"}</p>
                        <p className="truncate text-xs font-semibold text-slate-400">
                          {driver.vehicleModel ?? "Veículo não informado"}
                          {driver.vehiclePlate ? ` · ${driver.vehiclePlate}` : ""}
                        </p>
                      </div>
                      <span className={classNames(
                        "flex-shrink-0 rounded-xl px-2.5 py-1 text-xs font-black",
                        driver.available ? "bg-violet-50 text-violet-700" : "bg-amber-50 text-amber-700"
                      )}>
                        {driver.available ? "Livre" : "Em corrida"}
                      </span>
                    </div>
                  ))}
                </div>
              )
            )}
          </div>
        </div>
      </div>
    </SectionCard>
  );
}

const RIDE_STATUS_TABS: { value: RideStatus | ""; label: string }[] = [
  { value: "", label: "Todas" },
  { value: "PENDING_DRIVER", label: "Procurando" },
  { value: "ACCEPTED", label: "Aceita" },
  { value: "DRIVER_ARRIVING", label: "A caminho" },
  { value: "DRIVER_ARRIVED", label: "Chegou" },
  { value: "IN_PROGRESS", label: "Em andamento" },
  { value: "FINISHED", label: "Finalizada" },
  { value: "CANCELED", label: "Cancelada" },
];

function duration(seconds: number | null) {
  if (!seconds) return null;
  const m = Math.floor(seconds / 60);
  return m < 60 ? `${m} min` : `${Math.floor(m / 60)}h ${m % 60}min`;
}

function RidesPanel({
  rides,
  rideStatus,
  paymentStatus,
  onRideStatus,
  onPaymentStatus,
}: {
  rides: Ride[];
  rideStatus: string;
  paymentStatus: string;
  onRideStatus: (value: string) => void;
  onPaymentStatus: (value: string) => void;
}) {
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<Ride | null>(null);

  const query = search.trim().toLowerCase();
  const visible = query
    ? rides.filter(
        (r) =>
          (r.passenger?.name ?? "").toLowerCase().includes(query) ||
          (r.passenger?.phone ?? "").includes(query) ||
          (r.driver?.name ?? "").toLowerCase().includes(query) ||
          (r.driver?.phone ?? "").includes(query) ||
          (r.originAddress ?? "").toLowerCase().includes(query) ||
          (r.destinationAddress ?? "").toLowerCase().includes(query)
      )
    : rides;

  return (
    <SectionCard
      title="Corridas"
      subtitle="Histórico recente com status e pagamento."
    >
      {/* Tabs de status */}
      <div className="mb-3 -mx-1 flex flex-wrap gap-1.5 overflow-x-auto px-1 pb-1">
        {RIDE_STATUS_TABS.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => onRideStatus(value)}
            className={classNames(
              "shrink-0 rounded-xl px-3 py-1.5 text-xs font-black transition",
              rideStatus === value
                ? "bg-slate-900 text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Filtro de pagamento + busca */}
      <div className="mb-4 flex flex-wrap gap-2">
        <div className="relative flex-1 min-w-[200px]">
          <svg
            className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
            width="15" height="15" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por nome, telefone ou endereço..."
            className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-8 text-sm font-semibold outline-none focus:border-violet-500 focus:bg-white"
          />
          {search && (
            <button onClick={() => setSearch("")} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700">
              ✕
            </button>
          )}
        </div>
        <select
          value={paymentStatus}
          onChange={(e) => onPaymentStatus(e.target.value)}
          className="rounded-2xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-bold text-slate-700 outline-none"
        >
          <option value="">Todos pagamentos</option>
          {(["PENDING", "RECEIVED", "NOT_RECEIVED", "CANCELED"] as PaymentStatus[]).map((s) => (
            <option key={s} value={s}>{statusLabel(s)}</option>
          ))}
        </select>
      </div>

      {/* Tabela */}
      <div className="overflow-x-auto rounded-2xl border border-slate-200">
        <table className="w-full min-w-[680px] text-left text-sm">
          <thead>
            <tr className="bg-slate-50 text-xs font-black uppercase tracking-wide text-slate-400">
              <th className="px-4 py-3">Passageiro</th>
              <th className="px-4 py-3">Motorista</th>
              <th className="px-4 py-3">Rota</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Valor</th>
              <th className="px-4 py-3">Pagamento</th>
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center font-bold text-slate-400">
                  {query ? `Nenhuma corrida encontrada para "${search}".` : "Nenhuma corrida encontrada."}
                </td>
              </tr>
            )}
            {visible.map((ride) => (
              <tr
                key={ride.id}
                onClick={() => setSelected(ride)}
                className="cursor-pointer border-t border-slate-100 transition hover:bg-violet-50"
              >
                <td className="px-4 py-3">
                  <p className="font-bold text-slate-900">{ride.passenger?.name ?? "—"}</p>
                  <p className="text-xs font-medium text-slate-400">{ride.passenger?.phone}</p>
                </td>
                <td className="px-4 py-3">
                  {ride.driver ? (
                    <>
                      <p className="font-semibold text-slate-800">{ride.driver.name ?? "—"}</p>
                      <p className="text-xs font-medium text-slate-400">{ride.driver.phone}</p>
                    </>
                  ) : (
                    <span className="text-xs font-bold text-slate-300">Sem motorista</span>
                  )}
                </td>
                <td className="px-4 py-3 max-w-[200px]">
                  <p className="truncate text-xs font-semibold text-slate-700">{ride.originAddress ?? "—"}</p>
                  <p className="truncate text-xs font-medium text-slate-400">{ride.destinationAddress ?? "—"}</p>
                </td>
                <td className="px-4 py-3">
                  <Badge value={statusLabel(ride.status)} tone={statusTone(ride.status)} />
                </td>
                <td className="px-4 py-3">
                  <p className="font-black text-slate-900">{money(ride.estimatedFareCents)}</p>
                  {km(ride.distanceMeters) && <p className="text-xs font-medium text-slate-400">{km(ride.distanceMeters)}</p>}
                </td>
                <td className="px-4 py-3">
                  <Badge value={statusLabel(ride.paymentStatus)} tone={statusTone(ride.paymentStatus)} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {visible.length > 0 && (
        <p className="mt-2 text-xs font-bold text-slate-400">
          {visible.length}{query || rideStatus || paymentStatus ? ` de ${rides.length}` : ""} corrida{rides.length !== 1 ? "s" : ""}
        </p>
      )}

      {selected && (
        <RideDetailModal ride={selected} onClose={() => setSelected(null)} />
      )}
    </SectionCard>
  );
}

function RideDetailModal({ ride, onClose }: { ride: Ride; onClose: () => void }) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-3xl bg-white shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 px-6 py-5">
          <div className="flex flex-wrap gap-1.5">
            <Badge value={statusLabel(ride.status)} tone={statusTone(ride.status)} />
            <Badge value={statusLabel(ride.paymentStatus)} tone={statusTone(ride.paymentStatus)} />
            {ride.paymentMethod && (
              <span className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-bold text-slate-600">
                {ride.paymentMethod === "PIX" ? "PIX" : "Dinheiro"}
              </span>
            )}
          </div>
          <button onClick={onClose} className="shrink-0 rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-black text-slate-600 hover:bg-slate-200">
            Fechar
          </button>
        </div>

        <div className="grid gap-5 px-6 py-5">
          {/* Passageiro */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Passageiro</p>
            <div className="grid grid-cols-2 gap-3">
              <DetailField label="Nome" value={ride.passenger?.name ?? "—"} />
              <DetailField label="Telefone" value={ride.passenger?.phone ?? "—"} />
            </div>
          </div>

          {/* Motorista */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Motorista</p>
            {ride.driver ? (
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Nome" value={ride.driver.name ?? "—"} />
                <DetailField label="Telefone" value={ride.driver.phone ?? "—"} />
              </div>
            ) : (
              <p className="text-sm font-bold text-slate-400">Nenhum motorista atribuído</p>
            )}
          </div>

          {/* Rota */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Rota</p>
            <div className="grid gap-3">
              <DetailField label="Origem" value={ride.originAddress ?? "Não informado"} />
              <DetailField label="Destino" value={ride.destinationAddress ?? "Não informado"} />
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Distância" value={km(ride.distanceMeters) ?? "—"} />
                <DetailField label="Duração estimada" value={duration(ride.durationSeconds) ?? "—"} />
              </div>
            </div>
          </div>

          {/* Financeiro */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Financeiro</p>
            <div className="grid grid-cols-2 gap-3">
              <DetailField label="Valor estimado" value={money(ride.estimatedFareCents) ?? "—"} />
              <DetailField label="Método" value={ride.paymentMethod === "PIX" ? "PIX" : ride.paymentMethod === "CASH" ? "Dinheiro" : "—"} />
            </div>
          </div>

          {/* Datas */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Datas</p>
            <div className="grid grid-cols-2 gap-3">
              <DetailField label="Criada em" value={dateTime(ride.createdAt) ?? "—"} />
              <DetailField label="Atualizada em" value={dateTime(ride.updatedAt) ?? "—"} />
            </div>
          </div>

          {/* Avaliação */}
          {ride.review && (
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Avaliação do passageiro</p>
              <div className="rounded-2xl bg-amber-50 p-4">
                <div className="flex items-center gap-1 mb-1">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <span key={i} className={i < ride.review!.rating ? "text-amber-400" : "text-slate-200"}>★</span>
                  ))}
                  <span className="ml-1 text-sm font-black text-amber-700">{ride.review.rating}/5</span>
                </div>
                {ride.review.comment && (
                  <p className="text-sm font-medium text-slate-700 leading-relaxed">&quot;{ride.review.comment}&quot;</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function pct(num: number, den: number) {
  if (!den) return 0;
  return Math.round((num / den) * 100);
}

function ProgressBar({ value, color = "bg-violet-600" }: { value: number; color?: string }) {
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-slate-100">
      <div
        className={classNames("h-full rounded-full transition-all", color)}
        style={{ width: `${Math.min(100, Math.max(0, value))}%` }}
      />
    </div>
  );
}

function ReportKPI({
  label,
  value,
  sub,
  highlight,
}: {
  label: string;
  value: string | number;
  sub?: string;
  highlight?: "green" | "red" | "amber" | "blue";
}) {
  const valueColor =
    highlight === "green"
      ? "text-emerald-700"
      : highlight === "red"
      ? "text-red-600"
      : highlight === "amber"
      ? "text-amber-600"
      : highlight === "blue"
      ? "text-blue-700"
      : "text-slate-900";
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-black uppercase tracking-widest text-slate-400">{label}</p>
      <p className={classNames("mt-1 text-2xl font-black", valueColor)}>{value}</p>
      {sub && <p className="mt-0.5 text-xs font-semibold text-slate-400">{sub}</p>}
    </div>
  );
}

function ReportSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <p className="mb-3 text-xs font-black uppercase tracking-widest text-slate-400">{title}</p>
      {children}
    </div>
  );
}

function BreakdownRow({
  label,
  value,
  total,
  color,
  badge,
}: {
  label: string;
  value: number;
  total: number;
  color: string;
  badge?: string;
}) {
  const p = pct(value, total);
  return (
    <div className="grid gap-1">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm font-bold text-slate-700">{label}</span>
          {badge && (
            <span className={classNames(
              "rounded-full px-2 py-0.5 text-xs font-bold",
              badge === "Revisar" ? "bg-amber-100 text-amber-700" : "bg-red-100 text-red-700"
            )}>
              {badge}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-black text-slate-900">{value}</span>
          <span className="w-8 text-right text-xs font-bold text-slate-400">{p}%</span>
        </div>
      </div>
      <ProgressBar value={p} color={color} />
    </div>
  );
}

function ReportsPanel({
  reports,
  reportFrom,
  reportTo,
  onFromChange,
  onToChange,
  onFilter,
}: {
  reports: ReportData | null;
  reportFrom: string;
  reportTo: string;
  onFromChange: (v: string) => void;
  onToChange: (v: string) => void;
  onFilter: () => void;
}) {
  const r = reports;
  const [generatingPdf, setGeneratingPdf] = useState(false);

  const totalRides = r?.totalRides ?? 0;
  const finished = r?.finishedRides ?? 0;
  const canceled = r?.canceledRides ?? 0;
  const inProgress =
    (r?.ridesByStatus?.IN_PROGRESS ?? 0) +
    (r?.ridesByStatus?.ACCEPTED ?? 0) +
    (r?.ridesByStatus?.DRIVER_ARRIVING ?? 0) +
    (r?.ridesByStatus?.DRIVER_ARRIVED ?? 0) +
    (r?.ridesByStatus?.PENDING_DRIVER ?? 0);

  const received = r?.receivedPayments ?? 0;
  const notReceived = r?.notReceivedPayments ?? 0;
  const pending = r?.pendingPayments ?? 0;
  const totalPayments = received + notReceived + pending;

  const approved = r?.driversByApproval?.APPROVED ?? 0;
  const pendingDrivers = r?.driversByApproval?.PENDING ?? 0;
  const rejected = r?.driversByApproval?.REJECTED ?? 0;
  const totalDrivers = approved + pendingDrivers + rejected;

  const online = r?.onlineDrivers ?? 0;
  const available = Math.min(r?.availableDrivers ?? 0, online);
  const busy = Math.max(0, online - available);

  const completionRate = pct(finished, totalRides);
  const cancellationRate = pct(canceled, totalRides);
  const paymentRate = pct(received, totalPayments);

  const avgTicket =
    received > 0 && r?.receivedRevenueCents
      ? money(Math.round(r.receivedRevenueCents / received))
      : "—";

  const revenueGap =
    r?.estimatedFinishedFareCents != null && r?.receivedRevenueCents != null
      ? r.estimatedFinishedFareCents - r.receivedRevenueCents
      : null;

  const handleDownloadPdf = async () => {
    if (!r) return;
    setGeneratingPdf(true);
    try {
      await generateReportPdf({ r, from: reportFrom, to: reportTo });
    } finally {
      setGeneratingPdf(false);
    }
  };

  return (
    <SectionCard
      title="Relatórios"
      subtitle="Métricas operacionais, financeiras e de qualidade."
      action={
        <div className="flex items-center gap-3">
          {r?.generatedAt && (
            <span className="text-xs font-bold text-slate-400">
              Gerado em {dateTime(r.generatedAt)}
            </span>
          )}
          <button
            onClick={handleDownloadPdf}
            disabled={!r || generatingPdf}
            className={classNames(
              "flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-black transition-colors",
              !r || generatingPdf
                ? "cursor-not-allowed bg-slate-100 text-slate-400"
                : "bg-violet-600 text-white hover:bg-violet-700 shadow-sm"
            )}
          >
            {generatingPdf ? (
              <>
                <svg className="h-3.5 w-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                </svg>
                Gerando...
              </>
            ) : (
              <>
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                Baixar PDF
              </>
            )}
          </button>
        </div>
      }
    >
      {/* Filtro de período */}
      <div className="mb-6 flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
        <div className="grid gap-1">
          <span className="text-xs font-black uppercase tracking-widest text-slate-400">Período</span>
          <div className="flex items-center gap-2">
            <label className="grid gap-1">
              <span className="text-xs font-bold text-slate-500">De</span>
              <input
                type="date"
                value={reportFrom}
                onChange={(e) => onFromChange(e.target.value)}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500"
              />
            </label>
            <label className="grid gap-1">
              <span className="text-xs font-bold text-slate-500">Até</span>
              <input
                type="date"
                value={reportTo}
                onChange={(e) => onToChange(e.target.value)}
                className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500"
              />
            </label>
          </div>
        </div>
        <button
          onClick={onFilter}
          className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-black text-white hover:bg-slate-700"
        >
          Aplicar filtro
        </button>
        {(reportFrom || reportTo) && (
          <button
            onClick={() => { onFromChange(""); onToChange(""); setTimeout(onFilter, 0); }}
            className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-black text-slate-600 hover:bg-slate-50"
          >
            Limpar
          </button>
        )}
        {(reportFrom || reportTo) && (
          <span className="text-xs font-bold text-violet-700">
            Mostrando dados de {reportFrom || "início"} até {reportTo || "hoje"}
          </span>
        )}
      </div>

      {/* KPIs principais */}
      <ReportSection title="Destaques">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <ReportKPI
            label="Receita recebida"
            value={money(r?.receivedRevenueCents) ?? "—"}
            sub={`de ${money(r?.estimatedFinishedFareCents)} estimado`}
            highlight="green"
          />
          <ReportKPI
            label="Taxa de conclusão"
            value={`${completionRate}%`}
            sub={`${finished} de ${totalRides} corridas`}
            highlight={completionRate >= 80 ? "green" : completionRate >= 60 ? "amber" : "red"}
          />
          <ReportKPI
            label="Ticket médio"
            value={avgTicket}
            sub={`${received} pagamentos recebidos`}
            highlight="blue"
          />
          <ReportKPI
            label="Avaliação média"
            value={r?.averageRating != null ? `${r.averageRating.toFixed(1).replace(".", ",")} ★` : "—"}
            sub={`${r?.totalReviews ?? 0} avaliação${(r?.totalReviews ?? 0) !== 1 ? "ões" : ""}`}
            highlight={
              r?.averageRating != null
                ? r.averageRating >= 4.5
                  ? "green"
                  : r.averageRating >= 3.5
                  ? "amber"
                  : "red"
                : undefined
            }
          />
        </div>
      </ReportSection>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Corridas */}
        <ReportSection title="Corridas por status">
          <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4">
            <BreakdownRow label="Finalizadas" value={finished} total={totalRides} color="bg-emerald-500" />
            <BreakdownRow label="Em andamento" value={inProgress} total={totalRides} color="bg-blue-500" />
            <BreakdownRow label="Canceladas" value={canceled} total={totalRides} color="bg-red-400"
              badge={cancellationRate > 20 ? "Alto" : undefined}
            />
            <div className="border-t border-slate-100 pt-2 flex justify-between text-xs font-black text-slate-400">
              <span>Total</span>
              <span>{totalRides}</span>
            </div>
          </div>
        </ReportSection>

        {/* Pagamentos */}
        <ReportSection title="Pagamentos">
          <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4">
            <BreakdownRow label="Recebidos" value={received} total={totalPayments} color="bg-emerald-500" />
            <BreakdownRow label="Pendentes" value={pending} total={totalPayments} color="bg-amber-400" />
            <BreakdownRow label="Não recebidos" value={notReceived} total={totalPayments} color="bg-red-400"
              badge={notReceived > 0 ? "Atenção" : undefined}
            />
            {revenueGap != null && revenueGap > 0 && (
              <div className="rounded-xl bg-amber-50 px-3 py-2">
                <p className="text-xs font-bold text-amber-700">
                  Gap de receita: {money(revenueGap)} entre estimado e recebido
                </p>
              </div>
            )}
            <div className="border-t border-slate-100 pt-2 flex justify-between text-xs font-black text-slate-400">
              <span>Taxa de recebimento</span>
              <span>{paymentRate}%</span>
            </div>
          </div>
        </ReportSection>

        {/* Motoristas */}
        <ReportSection title="Base de motoristas">
          <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4">
            <BreakdownRow label="Aprovados" value={approved} total={totalDrivers} color="bg-violet-500" />
            <BreakdownRow label="Pendentes" value={pendingDrivers} total={totalDrivers} color="bg-amber-400"
              badge={pendingDrivers > 0 ? "Revisar" : undefined}
            />
            <BreakdownRow label="Rejeitados" value={rejected} total={totalDrivers} color="bg-slate-300" />
            <div className="border-t border-slate-100 pt-2 flex justify-between text-xs font-black text-slate-400">
              <span>Total cadastrados</span>
              <span>{totalDrivers}</span>
            </div>
          </div>
        </ReportSection>

        {/* Operação agora */}
        <ReportSection title="Operação em tempo real">
          <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4">
            <BreakdownRow label="Online" value={online} total={Math.max(online, approved)} color="bg-violet-500" />
            <BreakdownRow label="Disponíveis" value={available} total={Math.max(online, 1)} color="bg-emerald-400" />
            <BreakdownRow label="Ocupados" value={busy} total={Math.max(online, 1)} color="bg-amber-400" />
            <div className="border-t border-slate-100 pt-2 flex justify-between text-xs font-black text-slate-400">
              <span>Taxa de ocupação</span>
              <span>{pct(busy, online || 1)}%</span>
            </div>
          </div>
        </ReportSection>
      </div>

      {/* Chamados abertos */}
      {(r?.openSupportTickets ?? 0) > 0 && (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 flex items-center justify-between gap-3">
          <div>
            <p className="text-sm font-black text-amber-800">Chamados de suporte em aberto</p>
            <p className="text-xs font-semibold text-amber-600">Requerem atenção da equipe</p>
          </div>
          <span className="text-3xl font-black text-amber-700">{r?.openSupportTickets}</span>
        </div>
      )}
    </SectionCard>
  );
}

function DriversPanel({
  drivers,
  filter,
  onFilter,
  authFetch,
  onChanged,
}: {
  drivers: DriverProfile[];
  filter: "PENDING" | "APPROVED" | "REJECTED" | "ALL";
  onFilter: (value: "PENDING" | "APPROVED" | "REJECTED" | "ALL") => void;
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [search, setSearch] = useState("");

  const query = search.trim().toLowerCase();
  const visible = query
    ? drivers.filter((d) =>
        (d.user.name ?? "").toLowerCase().includes(query) ||
        d.user.phone.includes(query) ||
        (d.cnhNumber ?? "").toLowerCase().includes(query) ||
        (d.vehiclePlate ?? "").toLowerCase().includes(query) ||
        (d.vehicleModel ?? "").toLowerCase().includes(query)
      )
    : drivers;

  return (
    <SectionCard
      title="Motoristas"
      subtitle="Aprovação, rejeição e consulta da base de motoristas."
      action={
        <div className="flex flex-wrap gap-2">
          {(["PENDING", "APPROVED", "REJECTED", "ALL"] as const).map((item) => (
            <button
              key={item}
              onClick={() => onFilter(item)}
              className={classNames(
                "rounded-2xl px-4 py-2 text-sm font-black",
                filter === item ? "bg-slate-900 text-white" : "bg-slate-100 text-slate-600"
              )}
            >
              {item === "ALL" ? "Todos" : statusLabel(item)}
            </button>
          ))}
        </div>
      }
    >
      {/* Campo de busca */}
      <div className="relative mb-4">
        <svg
          className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
          width="16" height="16" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
        >
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nome, telefone, CNH ou placa..."
          className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-10 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500 focus:bg-white"
        />
        {search && (
          <button
            onClick={() => setSearch("")}
            className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700"
          >
            ✕
          </button>
        )}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        {visible.length === 0 && (
          <p className="font-bold text-slate-400">
            {query ? `Nenhum motorista encontrado para "${search}".` : "Nenhum motorista nesta lista."}
          </p>
        )}
        {visible.map((driver) => (
          <DriverCard key={driver.id} driver={driver} authFetch={authFetch} onChanged={onChanged} />
        ))}
      </div>

      {query && visible.length > 0 && (
        <p className="mt-3 text-xs font-bold text-slate-400">
          {visible.length} de {drivers.length} motorista{drivers.length !== 1 ? "s" : ""}
        </p>
      )}
    </SectionCard>
  );
}

function DriverCard({
  driver,
  authFetch,
  onChanged,
}: {
  driver: DriverProfile;
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <div className={classNames(
        "flex items-center justify-between gap-3 rounded-3xl border p-4",
        driver.approvalStatus === "PENDING"
          ? "border-amber-300 bg-amber-50"
          : "border-slate-200 bg-slate-50"
      )}>
        <div className="min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="truncate text-base font-black">{driver.user.name ?? "Sem nome"}</h3>
            <Badge value={statusLabel(driver.approvalStatus)} tone={statusTone(driver.approvalStatus)} />
            {driver.user.blocked && <Badge value="Bloqueado" tone="red" />}
          </div>
          <p className="text-sm font-semibold text-slate-500">{driver.user.phone}</p>
          <p className="mt-1 text-xs font-medium text-slate-400">
            {driver.vehicleModel ?? "Veículo não informado"}
            {driver.vehiclePlate ? ` · ${driver.vehiclePlate}` : ""}
            {driver.vehicleColor ? ` · ${driver.vehicleColor}` : ""}
          </p>
        </div>
        <button
          onClick={() => setOpen(true)}
          className="shrink-0 rounded-2xl bg-slate-900 px-4 py-2.5 text-xs font-black text-white"
        >
          Ver detalhes
        </button>
      </div>

      {open && (
        <DriverDetailModal
          driver={driver}
          authFetch={authFetch}
          onChanged={() => { onChanged(); setOpen(false); }}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}

function DriverDetailModal({
  driver,
  authFetch,
  onChanged,
  onClose,
}: {
  driver: DriverProfile;
  authFetch: AuthFetch;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [reason, setReason] = useState("");
  const [rejecting, setRejecting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [lightboxSrc, setLightboxSrc] = useState<string | null>(null);
  const [checklist, setChecklist] = useState({
    cpfVerified: false,
    cnhValid: false,
    cnhImageReviewed: false,
    vehicleDataOk: false,
    photoOk: false,
  });

  const photoUrl = driver.profilePhotoUrl ? `${API_URL}${driver.profilePhotoUrl}` : null;
  const cnhUrl   = driver.cnhImageUrl     ? `${API_URL}${driver.cnhImageUrl}`     : null;

  const checkItems: { key: keyof typeof checklist; label: string; sub: string; warn: boolean }[] = [
    { key: "cpfVerified",      label: "CPF verificado",               sub: driver.cpf ?? "Não informado",     warn: !driver.cpf },
    { key: "cnhValid",         label: "CNH e categoria conferidas",    sub: `Cat. ${driver.cnhCategory ?? "—"}  •  Vence ${driver.cnhExpiresAt ? dateTime(driver.cnhExpiresAt) : "não informado"}`, warn: !driver.cnhNumber || !driver.cnhCategory },
    { key: "cnhImageReviewed", label: "Foto da CNH analisada",        sub: cnhUrl ? "Imagem disponível" : "Sem imagem enviada", warn: !cnhUrl },
    { key: "vehicleDataOk",    label: "Dados do veículo confirmados", sub: `${driver.vehicleModel ?? "—"}  •  ${driver.vehiclePlate ?? "—"}`, warn: !driver.vehicleModel || !driver.vehiclePlate },
    { key: "photoOk",          label: "Foto de perfil adequada",      sub: photoUrl ? "Foto disponível" : "Sem foto de perfil", warn: !photoUrl },
  ];
  const checkedCount = Object.values(checklist).filter(Boolean).length;
  const allChecked   = checkedCount === checkItems.length;

  const approve = async () => {
    setBusy(true); setMessage("");
    try {
      await authFetch(`/admin/drivers/${driver.id}/approve`, { method: "PATCH" });
      onChanged();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Erro ao aprovar");
      setBusy(false);
    }
  };

  const reject = async () => {
    if (!reason.trim()) { setMessage("Informe o motivo da rejeição."); return; }
    setBusy(true); setMessage("");
    try {
      await authFetch(`/admin/drivers/${driver.id}/reject`, { method: "PATCH", body: JSON.stringify({ reason }) });
      onChanged();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Erro ao rejeitar");
      setBusy(false);
    }
  };

  const toggleBlock = async () => {
    const action = driver.user.blocked ? "unblock" : "block";
    if (!window.confirm(driver.user.blocked ? "Desbloquear a conta deste motorista?" : "Bloquear a conta deste motorista?")) return;
    setBusy(true); setMessage("");
    try {
      await authFetch(`/admin/users/${driver.userId}/${action}`, { method: "PATCH" });
      onChanged();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Erro ao atualizar conta");
      setBusy(false);
    }
  };

  return (
    <>
      {/* Lightbox */}
      {lightboxSrc && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/85 p-4 backdrop-blur-sm"
          onClick={() => setLightboxSrc(null)}
        >
          <div className="relative max-h-[92vh] max-w-3xl w-full" onClick={(e) => e.stopPropagation()}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={lightboxSrc} alt="Documento" className="max-h-[84vh] w-full rounded-3xl object-contain shadow-2xl" />
            <button
              onClick={() => setLightboxSrc(null)}
              className="absolute -top-3 -right-3 flex h-9 w-9 items-center justify-center rounded-full bg-white text-lg font-black text-slate-800 shadow-lg hover:bg-red-50"
            >✕</button>
            {/* Download button */}
            <button
              onClick={() => {
                const isCnh = lightboxSrc === cnhUrl;
                const ext = lightboxSrc.split(".").pop()?.split("?")[0] ?? "jpg";
                const filename = isCnh
                  ? `CNH_${driver.user.name?.replace(/\s+/g, "_") ?? driver.userId}.${ext}`
                  : `Foto_${driver.user.name?.replace(/\s+/g, "_") ?? driver.userId}.${ext}`;
                downloadFile(lightboxSrc, filename);
              }}
              className="absolute -bottom-3 left-1/2 -translate-x-1/2 flex items-center gap-1.5 rounded-full bg-white px-4 py-1.5 text-xs font-black text-slate-800 shadow-lg hover:bg-violet-50 hover:text-violet-700 transition-colors"
            >
              <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              Baixar imagem
            </button>
          </div>
        </div>
      )}

      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
        onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
      >
        <div className="w-full max-w-xl max-h-[92vh] overflow-y-auto rounded-3xl bg-white shadow-2xl">

          {/* ── Header com foto ── */}
          <div className="relative overflow-hidden rounded-t-3xl bg-gradient-to-br from-violet-700 to-violet-500 px-6 py-6">
            <div className="flex items-center gap-4">
              {/* Avatar */}
              <div className="relative flex-shrink-0">
                {photoUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={photoUrl}
                    alt="Foto"
                    className="h-16 w-16 cursor-pointer rounded-2xl border-2 border-white/30 object-cover shadow-lg"
                    onClick={() => setLightboxSrc(photoUrl)}
                  />
                ) : (
                  <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/20 text-2xl font-black text-white">
                    {(driver.user.name ?? "?")[0]?.toUpperCase()}
                  </div>
                )}
                {driver.online && (
                  <span className="absolute -bottom-1 -right-1 h-4 w-4 rounded-full border-2 border-white bg-emerald-400" />
                )}
              </div>
              {/* Info */}
              <div className="min-w-0 flex-1">
                <h2 className="truncate text-xl font-black text-white">{driver.user.name ?? "Sem nome"}</h2>
                <p className="text-sm font-semibold text-white/70">{driver.user.phone}</p>
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  <span className={classNames(
                    "rounded-lg px-2.5 py-0.5 text-xs font-black",
                    driver.approvalStatus === "APPROVED" ? "bg-emerald-400/25 text-white" :
                    driver.approvalStatus === "PENDING"  ? "bg-amber-400/25 text-white"   :
                    "bg-red-400/25 text-white"
                  )}>
                    {statusLabel(driver.approvalStatus)}
                  </span>
                  {driver.user.blocked && (
                    <span className="rounded-lg bg-red-500/30 px-2.5 py-0.5 text-xs font-black text-white">Bloqueado</span>
                  )}
                  <span className={classNames(
                    "rounded-lg px-2.5 py-0.5 text-xs font-bold",
                    driver.online ? "bg-white/20 text-white" : "bg-white/10 text-white/50"
                  )}>
                    {driver.online ? (driver.available ? "Online • Disponível" : "Online • Em corrida") : "Offline"}
                  </span>
                </div>
              </div>
            </div>
            {/* Fechar */}
            <button
              onClick={onClose}
              className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-xl bg-white/20 text-sm font-black text-white hover:bg-white/30"
            >✕</button>
          </div>

          <div className="grid gap-6 px-6 py-6">

            {/* ── Desempenho ── */}
            <div className="grid grid-cols-3 gap-3">
              <div className="rounded-2xl bg-slate-50 px-3 py-3 text-center">
                <p className="text-xs font-bold text-slate-400">Corridas</p>
                <p className="mt-0.5 text-xl font-black text-slate-900">{driver.totalFinishedRides}</p>
              </div>
              <div className="rounded-2xl bg-amber-50 px-3 py-3 text-center">
                <p className="text-xs font-bold text-amber-400">Avaliação</p>
                <p className="mt-0.5 text-xl font-black text-amber-600">
                  {driver.averageRating != null ? `${driver.averageRating.toFixed(1).replace(".", ",")} ★` : "—"}
                </p>
              </div>
              <div className="rounded-2xl bg-slate-50 px-3 py-3 text-center">
                <p className="text-xs font-bold text-slate-400">Reviews</p>
                <p className="mt-0.5 text-xl font-black text-slate-900">{driver.totalReviews}</p>
              </div>
            </div>

            {/* ── Checklist de verificação ── */}
            {driver.approvalStatus === "PENDING" && (
              <div className="rounded-2xl border border-violet-200 bg-violet-50 p-4">
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-xs font-black uppercase tracking-widest text-violet-600">Checklist de verificação</p>
                  <span className="rounded-lg bg-violet-100 px-2.5 py-0.5 text-xs font-black text-violet-700">
                    {checkedCount}/{checkItems.length}
                  </span>
                </div>
                <div className="grid gap-2">
                  {checkItems.map((item) => (
                    <label
                      key={item.key}
                      className={classNames(
                        "flex cursor-pointer items-start gap-3 rounded-xl border p-3 transition-colors",
                        checklist[item.key]
                          ? "border-emerald-200 bg-emerald-50"
                          : item.warn
                            ? "border-amber-200 bg-amber-50"
                            : "border-slate-200 bg-white hover:bg-slate-50"
                      )}
                    >
                      <input
                        type="checkbox"
                        checked={checklist[item.key]}
                        onChange={(e) => setChecklist(prev => ({ ...prev, [item.key]: e.target.checked }))}
                        className="mt-0.5 h-4 w-4 accent-violet-600"
                      />
                      <div className="min-w-0 flex-1">
                        <p className={classNames(
                          "text-sm font-black",
                          checklist[item.key] ? "text-emerald-800" : item.warn ? "text-amber-800" : "text-slate-800"
                        )}>{item.label}</p>
                        <p className={classNames(
                          "mt-0.5 truncate text-xs font-semibold",
                          checklist[item.key] ? "text-emerald-600" : item.warn ? "text-amber-600" : "text-slate-500"
                        )}>{item.sub}</p>
                      </div>
                      {item.warn && !checklist[item.key] && (
                        <span className="flex-shrink-0 text-base">⚠️</span>
                      )}
                      {checklist[item.key] && (
                        <span className="flex-shrink-0 text-base">✅</span>
                      )}
                    </label>
                  ))}
                </div>
                {!allChecked && (
                  <p className="mt-3 text-center text-xs font-semibold text-violet-500">
                    Marque todos os itens para liberar a aprovação
                  </p>
                )}
              </div>
            )}

            {/* ── Documentos: Fotos ── */}
            {(cnhUrl || driver.approvalStatus === "PENDING") && (
              <div>
                <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Documentos</p>
                <div className="grid grid-cols-2 gap-3">
                  {/* Foto CNH */}
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 overflow-hidden">
                    <div className="flex items-center justify-between px-3 pt-2.5">
                      <p className="text-xs font-black text-slate-500">Foto da CNH</p>
                      {cnhUrl && (
                        <button
                          onClick={() => {
                            const ext = cnhUrl.split(".").pop()?.split("?")[0] ?? "jpg";
                            downloadFile(cnhUrl, `CNH_${driver.user.name?.replace(/\s+/g, "_") ?? driver.userId}.${ext}`);
                          }}
                          title="Baixar foto da CNH"
                          className="flex items-center gap-1 rounded-lg bg-violet-50 px-2 py-1 text-[10px] font-black text-violet-700 hover:bg-violet-100 transition-colors"
                        >
                          <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                          </svg>
                          Baixar
                        </button>
                      )}
                    </div>
                    {cnhUrl ? (
                      <div
                        className="relative m-2 cursor-zoom-in overflow-hidden rounded-xl"
                        onClick={() => setLightboxSrc(cnhUrl)}
                      >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={cnhUrl} alt="CNH" className="h-28 w-full rounded-xl object-cover" />
                        <div className="absolute inset-0 flex items-center justify-center rounded-xl bg-black/0 transition-colors hover:bg-black/30">
                          <span className="hidden text-xs font-black text-white group-hover:block">🔍 Ampliar</span>
                        </div>
                        <span className="absolute bottom-1.5 right-1.5 rounded-lg bg-black/60 px-2 py-0.5 text-[10px] font-bold text-white">Clique p/ ampliar</span>
                      </div>
                    ) : (
                      <div className="m-2 flex h-28 items-center justify-center rounded-xl bg-amber-50 border border-dashed border-amber-300">
                        <div className="text-center">
                          <p className="text-2xl">📄</p>
                          <p className="mt-1 text-xs font-bold text-amber-600">Não enviada</p>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Foto de perfil */}
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 overflow-hidden">
                    <p className="px-3 pt-2.5 text-xs font-black text-slate-500">Foto de perfil</p>
                    {photoUrl ? (
                      <div
                        className="relative m-2 cursor-zoom-in overflow-hidden rounded-xl"
                        onClick={() => setLightboxSrc(photoUrl)}
                      >
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={photoUrl} alt="Perfil" className="h-28 w-full rounded-xl object-cover" />
                        <span className="absolute bottom-1.5 right-1.5 rounded-lg bg-black/60 px-2 py-0.5 text-[10px] font-bold text-white">Clique p/ ampliar</span>
                      </div>
                    ) : (
                      <div className="m-2 flex h-28 items-center justify-center rounded-xl bg-slate-100 border border-dashed border-slate-300">
                        <div className="text-center">
                          <p className="text-2xl">👤</p>
                          <p className="mt-1 text-xs font-bold text-slate-400">Sem foto</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* ── Habilitação ── */}
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Habilitação</p>
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="CPF" value={driver.cpf ?? "—"} highlight={!driver.cpf} />
                <DetailField label="Número CNH" value={driver.cnhNumber ?? "—"} highlight={!driver.cnhNumber} />
                <DetailField label="Categoria" value={driver.cnhCategory ?? "—"} />
                <DetailField label="Validade CNH" value={driver.cnhExpiresAt ? dateTime(driver.cnhExpiresAt) : "—"} />
                <DetailField label="EAR" value={driver.hasEar === true ? "Sim" : driver.hasEar === false ? "Não" : "—"} />
              </div>
            </div>

            {/* ── Veículo ── */}
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Veículo</p>
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Modelo" value={driver.vehicleModel ?? "—"} highlight={!driver.vehicleModel} />
                <DetailField label="Placa" value={driver.vehiclePlate ?? "—"} highlight={!driver.vehiclePlate} />
                <DetailField label="Cor" value={driver.vehicleColor ?? "—"} />
                <DetailField label="Ano" value={driver.vehicleYear != null ? String(driver.vehicleYear) : "—"} />
                <DetailField label="Capacidade" value={driver.vehicleCapacity != null ? `${driver.vehicleCapacity} passageiros` : "—"} />
              </div>
            </div>

            {/* ── Pix ── */}
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Recebimento Pix</p>
              <div className={classNames(
                "rounded-2xl border px-4 py-3",
                driver.pixKey || driver.pixQrPayload
                  ? "border-emerald-200 bg-emerald-50"
                  : "border-amber-200 bg-amber-50"
              )}>
                {driver.pixKey || driver.pixQrPayload ? (
                  <>
                    <p className="text-xs font-black text-emerald-700">✅ Pix configurado</p>
                    {driver.pixKey && <p className="mt-1 truncate text-sm font-semibold text-emerald-800">{driver.pixKey}</p>}
                  </>
                ) : (
                  <p className="text-xs font-black text-amber-700">⚠️ Pix não configurado — motorista não poderá receber por Pix</p>
                )}
              </div>
            </div>

            {/* ── Registro ── */}
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Registro</p>
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Cadastrado em" value={dateTime(driver.createdAt)} />
                {driver.approvedAt && <DetailField label="Aprovado em" value={dateTime(driver.approvedAt)} />}
                {driver.updatedAt  && <DetailField label="Atualizado em" value={dateTime(driver.updatedAt)} />}
              </div>
            </div>

            {/* ── Motivo de rejeição ── */}
            {driver.rejectionReason && (
              <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
                <p className="text-xs font-black uppercase tracking-widest text-red-500">Motivo da rejeição</p>
                <p className="mt-1 text-sm font-semibold text-red-800">{driver.rejectionReason}</p>
              </div>
            )}

            {/* ── Feedback ── */}
            {message && (
              <p className="rounded-2xl bg-red-50 p-3 text-sm font-bold text-red-700">{message}</p>
            )}

            {/* ── Ações de aprovação ── */}
            {!rejecting && driver.approvalStatus !== "APPROVED" && (
              <div className="grid grid-cols-2 gap-3 border-t border-slate-100 pt-2">
                <button
                  onClick={approve}
                  disabled={busy || (driver.approvalStatus === "PENDING" && !allChecked)}
                  title={!allChecked ? "Conclua o checklist de verificação primeiro" : ""}
                  className="rounded-2xl bg-emerald-600 py-3 font-black text-white transition-opacity disabled:opacity-50"
                >
                  {busy ? "..." : "✓ Aprovar"}
                </button>
                <button
                  onClick={() => setRejecting(true)}
                  disabled={busy}
                  className="rounded-2xl border border-red-300 py-3 font-black text-red-600 transition-colors hover:bg-red-50 disabled:opacity-50"
                >
                  Rejeitar
                </button>
              </div>
            )}
            {!rejecting && driver.approvalStatus === "APPROVED" && (
              <div className="border-t border-slate-100 pt-2">
                <button
                  onClick={() => setRejecting(true)}
                  disabled={busy}
                  className="w-full rounded-2xl border border-red-300 py-3 font-black text-red-600 hover:bg-red-50 disabled:opacity-50"
                >
                  Revogar aprovação
                </button>
              </div>
            )}

            {/* ── Formulário de rejeição ── */}
            {rejecting && (
              <div className="grid gap-3 border-t border-slate-100 pt-2">
                <p className="text-sm font-black text-slate-700">Motivo da rejeição</p>
                <p className="text-xs font-semibold text-slate-500">
                  Este texto será exibido ao motorista no aplicativo.
                </p>
                <textarea
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Ex: CNH vencida, foto ilegível, placa não confere com os dados informados..."
                  rows={4}
                  className="rounded-2xl border border-slate-200 p-3 text-sm font-semibold outline-none focus:border-red-500 focus:ring-2 focus:ring-red-100"
                />
                <div className="grid grid-cols-2 gap-2">
                  <button
                    onClick={reject}
                    disabled={busy || !reason.trim()}
                    className="rounded-2xl bg-red-600 py-3 font-black text-white disabled:opacity-50"
                  >
                    {busy ? "..." : "Confirmar rejeição"}
                  </button>
                  <button
                    onClick={() => { setRejecting(false); setReason(""); }}
                    className="rounded-2xl bg-slate-100 py-3 font-black text-slate-700 hover:bg-slate-200"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}

            {/* ── Bloquear / Desbloquear ── */}
            {!rejecting && (
              <div className="border-t border-slate-100 pt-2">
                <button
                  onClick={toggleBlock}
                  disabled={busy}
                  className={classNames(
                    "w-full rounded-2xl py-2.5 text-xs font-black transition-colors disabled:opacity-50",
                    driver.user.blocked
                      ? "border border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                      : "border border-slate-200 text-slate-400 hover:border-red-200 hover:bg-red-50 hover:text-red-600"
                  )}
                >
                  {driver.user.blocked ? "🔓 Desbloquear conta" : "Bloquear conta do motorista"}
                </button>
                {driver.user.blocked && (
                  <p className="mt-1.5 text-center text-xs font-semibold text-red-500">
                    Conta bloqueada — motorista não consegue acessar o app
                  </p>
                )}
              </div>
            )}

          </div>
        </div>
      </div>
    </>
  );
}

function DetailField({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={classNames("rounded-2xl px-4 py-3", highlight ? "bg-amber-50 ring-1 ring-amber-300" : "bg-slate-50")}>
      <p className={classNames("text-xs font-bold", highlight ? "text-amber-500" : "text-slate-400")}>{label}</p>
      <p className={classNames("mt-0.5 text-sm font-black", highlight ? "text-amber-700" : "text-slate-800")}>{value}</p>
    </div>
  );
}

function Toggle({ checked, onChange, label }: { checked: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <label className="flex cursor-pointer items-center gap-3">
      <div className="relative" onClick={() => onChange(!checked)}>
        <div className={classNames("h-6 w-11 rounded-full transition-colors duration-200", checked ? "bg-violet-600" : "bg-slate-200")} />
        <div className={classNames("absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform duration-200", checked ? "translate-x-5" : "translate-x-0")} />
      </div>
      <span className="text-sm font-bold text-slate-700">{label}</span>
    </label>
  );
}

function MoneyField({ label, value, onChange, hint }: { label: string; value: string; onChange: (v: string) => void; hint?: string }) {
  return (
    <label className="grid gap-1">
      <span className="text-sm font-black text-slate-600">{label}</span>
      {hint && <span className="text-xs font-medium text-slate-400">{hint}</span>}
      <div className="flex items-center overflow-hidden rounded-2xl border border-slate-200 bg-white focus-within:border-violet-600">
        <span className="select-none border-r border-slate-100 px-3 py-3 text-sm font-bold text-slate-400">R$</span>
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="0,00"
          className="flex-1 bg-transparent px-3 py-3 font-semibold text-slate-900 outline-none"
        />
      </div>
    </label>
  );
}

function PricingPreview({ form }: { form: { baseFare: string; perKm: string; perMinute: string; minimum: string; bookingFee: string; surge: string } }) {
  const base = reaisToCents(form.baseFare);
  const perKm = reaisToCents(form.perKm);
  const perMin = reaisToCents(form.perMinute);
  const min = reaisToCents(form.minimum);
  const booking = reaisToCents(form.bookingFee);
  const surge = Math.max(0.5, Number(form.surge) || 1);
  const calc = (km: number, mins: number) =>
    Math.round(Math.max(min, (base + perKm * km + perMin * mins) * surge) + booking);
  return (
    <div className="rounded-2xl border border-violet-200 bg-violet-50 p-4">
      <p className="text-xs font-black uppercase tracking-widest text-violet-700">Simulação de corrida</p>
      <div className="mt-3 grid grid-cols-3 gap-2">
        {([{ km: 3, mins: 8, label: "Curta" }, { km: 7, mins: 15, label: "Média" }, { km: 15, mins: 30, label: "Longa" }]).map(({ km, mins, label }) => (
          <div key={label} className="rounded-xl bg-white p-3 text-center shadow-sm">
            <p className="text-xs font-bold text-slate-500">{label}</p>
            <p className="text-xs text-slate-400">{km} km · {mins} min</p>
            <p className="mt-1 text-lg font-black text-violet-800">{money(calc(km, mins))}</p>
          </div>
        ))}
      </div>
      {surge > 1 && <p className="mt-2 text-xs font-bold text-amber-600">Surge ×{surge.toFixed(1).replace(".", ",")} aplicado nos valores acima</p>}
    </div>
  );
}

function CuponsPanel({
  promoCodes,
  authFetch,
  onChanged,
}: {
  promoCodes: PromoCode[];
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [code, setCode] = useState("");
  const [discountType, setDiscountType] = useState<"percent" | "cents">("percent");
  const [discountValue, setDiscountValue] = useState("");
  const [maxUses, setMaxUses] = useState("");
  const [expiresAt, setExpiresAt] = useState("");
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState("");

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setFormError("");
    if (!code.trim()) { setFormError("Informe o código."); return; }
    const val = Number(discountValue);
    if (!discountValue || isNaN(val) || val <= 0) { setFormError("Informe um valor de desconto válido."); return; }
    setCreating(true);
    try {
      await authFetch("/admin/promo-codes", {
        method: "POST",
        body: JSON.stringify({
          code: code.trim().toUpperCase(),
          discountPercent: discountType === "percent" ? val : null,
          discountCents: discountType === "cents" ? Math.round(val * 100) : null,
          maxUses: maxUses ? Number(maxUses) : null,
          expiresAt: expiresAt || null,
        }),
      });
      setCode(""); setDiscountValue(""); setMaxUses(""); setExpiresAt("");
      onChanged();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Erro ao criar cupom.");
    } finally {
      setCreating(false);
    }
  };

  const handleToggle = async (id: string) => {
    try {
      await authFetch(`/admin/promo-codes/${id}/toggle`, { method: "PATCH" });
      onChanged();
    } catch {/* ignore */}
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Excluir este cupom?")) return;
    try {
      await authFetch(`/admin/promo-codes/${id}`, { method: "DELETE" });
      onChanged();
    } catch {/* ignore */}
  };

  const fmtDiscount = (p: PromoCode) => {
    if (p.discountPercent != null) return `${p.discountPercent}%`;
    if (p.discountCents != null) return `R$ ${(p.discountCents / 100).toFixed(2).replace(".", ",")}`;
    return "—";
  };

  return (
    <SectionCard title="Cupons de Desconto" subtitle="Gerencie códigos promocionais para passageiros">
      {/* Create form */}
      <form onSubmit={handleCreate} className="mb-6 rounded-xl border border-slate-200 bg-slate-50 p-4">
        <p className="mb-3 text-sm font-semibold text-slate-700">Novo cupom</p>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Código</label>
            <input
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-mono uppercase tracking-widest text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="EX: PROMO10"
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Tipo de desconto</label>
            <select
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={discountType}
              onChange={(e) => setDiscountType(e.target.value as "percent" | "cents")}
            >
              <option value="percent">Percentual (%)</option>
              <option value="cents">Valor fixo (R$)</option>
            </select>
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">
              {discountType === "percent" ? "Desconto (%)" : "Desconto (R$)"}
            </label>
            <input
              type="number"
              min="0"
              step={discountType === "percent" ? "1" : "0.01"}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder={discountType === "percent" ? "10" : "5.00"}
              value={discountValue}
              onChange={(e) => setDiscountValue(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Máx. usos (opcional)</label>
            <input
              type="number"
              min="1"
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Ex: 100"
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500">Validade (opcional)</label>
            <input
              type="date"
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
            />
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              disabled={creating}
              className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {creating ? "Criando…" : "Criar cupom"}
            </button>
          </div>
        </div>
        {formError && <p className="mt-2 text-xs font-medium text-red-600">{formError}</p>}
      </form>

      {/* Table */}
      {promoCodes.length === 0 ? (
        <p className="py-8 text-center text-sm text-slate-400">Nenhum cupom cadastrado.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                <th className="pb-2 pr-4">Código</th>
                <th className="pb-2 pr-4">Desconto</th>
                <th className="pb-2 pr-4">Usos</th>
                <th className="pb-2 pr-4">Validade</th>
                <th className="pb-2 pr-4">Status</th>
                <th className="pb-2">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {promoCodes.map((pc) => {
                const expired = pc.expiresAt ? new Date(pc.expiresAt) < new Date() : false;
                const exhausted = pc.maxUses != null && pc.usedCount >= pc.maxUses;
                return (
                  <tr key={pc.id} className="py-2">
                    <td className="py-3 pr-4 font-mono font-bold tracking-widest text-slate-900">{pc.code}</td>
                    <td className="py-3 pr-4 text-slate-700">{fmtDiscount(pc)}</td>
                    <td className="py-3 pr-4 text-slate-600">
                      {pc.usedCount}{pc.maxUses != null ? ` / ${pc.maxUses}` : ""}
                    </td>
                    <td className="py-3 pr-4 text-slate-600">
                      {pc.expiresAt ? new Date(pc.expiresAt).toLocaleDateString("pt-BR") : "—"}
                    </td>
                    <td className="py-3 pr-4">
                      {expired ? (
                        <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-500">Expirado</span>
                      ) : exhausted ? (
                        <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-semibold text-orange-600">Esgotado</span>
                      ) : pc.active ? (
                        <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-semibold text-green-700">Ativo</span>
                      ) : (
                        <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-600">Inativo</span>
                      )}
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleToggle(pc.id)}
                          className={`rounded-lg px-3 py-1 text-xs font-semibold transition-colors ${pc.active ? "bg-slate-100 text-slate-700 hover:bg-slate-200" : "bg-green-100 text-green-700 hover:bg-green-200"}`}
                        >
                          {pc.active ? "Desativar" : "Ativar"}
                        </button>
                        <button
                          onClick={() => handleDelete(pc.id)}
                          className="rounded-lg bg-red-100 px-3 py-1 text-xs font-semibold text-red-700 hover:bg-red-200"
                        >
                          Excluir
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </SectionCard>
  );
}

function TariffsPanel({
  pricing,
  regions,
  authFetch,
  onChanged,
}: {
  pricing: PricingConfig[];
  regions: OperationRegion[];
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [tab, setTab] = useState<"regions" | "pricing">("regions");
  const [editingRegionId, setEditingRegionId] = useState<string | null>(null);
  const [regionForm, setRegionForm] = useState({
    name: "",
    city: "Belo Horizonte",
    centerLat: -19.9191,
    centerLng: -43.9386,
    radiusMeters: 5000,
    active: true,
  });
  const [pricingForm, setPricingForm] = useState({
    name: "Tarifa padrão",
    regionId: "",
    baseFare: "5.00",
    perKm: "2.00",
    perMinute: "0.50",
    minimum: "8.00",
    bookingFee: "0.00",
    surge: "1",
  });
  const [toast, setToast] = useState<{ msg: string; ok: boolean } | null>(null);
  const [busyAction, setBusyAction] = useState<string | null>(null);

  const showToast = (msg: string, ok: boolean) => {
    setToast({ msg, ok });
    setTimeout(() => setToast(null), 3500);
  };

  const resetRegionForm = () => {
    setEditingRegionId(null);
    setRegionForm({ name: "", city: "Belo Horizonte", centerLat: -19.9191, centerLng: -43.9386, radiusMeters: 5000, active: true });
  };

  const editRegion = (region: OperationRegion) => {
    setEditingRegionId(region.id);
    setRegionForm({
      name: region.name,
      city: region.city ?? "",
      centerLat: region.centerLat ?? -19.9191,
      centerLng: region.centerLng ?? -43.9386,
      radiusMeters: region.radiusMeters ?? 5000,
      active: region.active,
    });
  };

  const saveRegion = async () => {
    if (!regionForm.name.trim()) { showToast("Informe o nome da região.", false); return; }
    setBusyAction("region");
    try {
      await authFetch(editingRegionId ? `/admin/regions/${editingRegionId}` : "/admin/regions", {
        method: editingRegionId ? "PATCH" : "POST",
        body: JSON.stringify(regionForm),
      });
      showToast(editingRegionId ? "Região atualizada com sucesso." : "Região criada com sucesso.", true);
      resetRegionForm();
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao salvar região", false);
    } finally {
      setBusyAction(null);
    }
  };

  const savePricing = async () => {
    if (!pricingForm.name.trim()) { showToast("Informe o nome da tarifa.", false); return; }
    setBusyAction("pricing");
    try {
      await authFetch("/admin/pricing", {
        method: "POST",
        body: JSON.stringify({
          name: pricingForm.name,
          regionId: pricingForm.regionId || null,
          baseFareCents: reaisToCents(pricingForm.baseFare),
          perKmCents: reaisToCents(pricingForm.perKm),
          perMinuteCents: reaisToCents(pricingForm.perMinute),
          minimumFareCents: reaisToCents(pricingForm.minimum),
          bookingFeeCents: reaisToCents(pricingForm.bookingFee),
          surgeMultiplier: Number(pricingForm.surge) || 1,
          isActive: true,
          currency: "BRL",
        }),
      });
      showToast("Tarifa salva e ativada com sucesso.", true);
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao salvar tarifa", false);
    } finally {
      setBusyAction(null);
    }
  };

  const toggleRegion = async (region: OperationRegion) => {
    setBusyAction(`region-${region.id}`);
    try {
      await authFetch(`/admin/regions/${region.id}`, {
        method: "PATCH",
        body: JSON.stringify({ active: !region.active }),
      });
      showToast(region.active ? "Região pausada." : "Região ativada.", true);
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao atualizar região", false);
    } finally {
      setBusyAction(null);
    }
  };

  const deleteRegion = async (region: OperationRegion) => {
    if (!window.confirm(`Excluir a região "${region.name}"? Todas as tarifas inativas vinculadas também serão removidas. Esta ação não pode ser desfeita.`)) return;
    setBusyAction(`delete-region-${region.id}`);
    try {
      await authFetch(`/admin/regions/${region.id}`, { method: "DELETE" });
      showToast("Região excluída com sucesso.", true);
      if (editingRegionId === region.id) resetRegionForm();
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao excluir região", false);
    } finally {
      setBusyAction(null);
    }
  };

  const activatePricing = async (item: PricingConfig) => {
    setBusyAction(`pricing-${item.id}`);
    try {
      await authFetch(`/admin/pricing/${item.id}/activate`, { method: "PATCH" });
      showToast("Tarifa ativada. As outras tarifas desta região foram desativadas.", true);
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao ativar tarifa", false);
    } finally {
      setBusyAction(null);
    }
  };

  const deactivatePricing = async (item: PricingConfig) => {
    setBusyAction(`deactivate-${item.id}`);
    try {
      await authFetch(`/admin/pricing/${item.id}/deactivate`, { method: "PATCH" });
      showToast("Tarifa desativada. Nenhuma tarifa ativa nesta região.", true);
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao desativar tarifa", false);
    } finally {
      setBusyAction(null);
    }
  };

  const deletePricing = async (item: PricingConfig) => {
    if (!window.confirm(`Excluir a tarifa "${item.name ?? "sem nome"}"? Esta ação não pode ser desfeita.`)) return;
    setBusyAction(`delete-${item.id}`);
    try {
      await authFetch(`/admin/pricing/${item.id}`, { method: "DELETE" });
      showToast("Tarifa excluída.", true);
      onChanged();
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Erro ao excluir tarifa", false);
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <SectionCard
      title="Regiões e Tarifas"
      subtitle="Configure onde o app opera e quanto cada corrida custa."
      action={
        toast ? (
          <div className={classNames(
            "rounded-2xl px-4 py-2 text-sm font-bold",
            toast.ok ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"
          )}>
            {toast.msg}
          </div>
        ) : undefined
      }
    >
      {/* Tabs */}
      <div className="mb-6 flex gap-1 rounded-2xl bg-slate-100 p-1">
        {([
          { key: "regions", label: "Regiões operacionais", count: regions.length },
          { key: "pricing", label: "Tarifas", count: pricing.length },
        ] as const).map(({ key, label, count }) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={classNames(
              "flex flex-1 items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-black transition-all",
              tab === key ? "bg-white text-slate-900 shadow-sm" : "text-slate-500 hover:text-slate-700"
            )}
          >
            {label}
            <span className={classNames(
              "rounded-full px-2 py-0.5 text-xs",
              tab === key ? "bg-slate-100 text-slate-600" : "bg-slate-200 text-slate-500"
            )}>{count}</span>
          </button>
        ))}
      </div>

      {/* ── Regioes ── */}
      {tab === "regions" && (
        <div className="grid gap-6 xl:grid-cols-[1.3fr_0.7fr]">
          {/* Coluna esquerda: mapa + lista */}
          <div className="grid gap-4">
            <div className="relative z-0 h-[420px] overflow-hidden rounded-3xl border border-slate-200 [&_.leaflet-container]:rounded-3xl">
              <RegionMap
                regions={regions}
                centerLat={regionForm.centerLat}
                centerLng={regionForm.centerLng}
                radiusMeters={regionForm.radiusMeters}
                onMapClick={(lat, lng) => setRegionForm((c) => ({ ...c, centerLat: lat, centerLng: lng }))}
                onRegionClick={editRegion}
              />
            </div>
            <p className="text-center text-xs font-semibold text-slate-400">
              Clique no mapa para posicionar o centro · Clique em uma região existente para editá-la
            </p>

            <div className="grid gap-2">
              <h3 className="text-xs font-black uppercase tracking-widest text-slate-400">Regiões cadastradas</h3>
              {regions.length === 0 && (
                <p className="rounded-2xl border border-dashed border-slate-200 p-6 text-center text-sm font-bold text-slate-400">
                  Nenhuma região cadastrada ainda.
                </p>
              )}
              {regions.map((region) => (
                <div
                  key={region.id}
                  className={classNames(
                    "flex items-center gap-3 rounded-2xl border p-4 transition-colors",
                    editingRegionId === region.id ? "border-violet-300 bg-violet-50" : "border-slate-200 bg-white hover:border-slate-300"
                  )}
                >
                  <div className={classNames("h-3 w-3 flex-shrink-0 rounded-full", region.active ? "bg-violet-500" : "bg-slate-300")} />
                  <button onClick={() => editRegion(region)} className="min-w-0 flex-1 text-left">
                    <p className="truncate font-black text-slate-900">{region.name}</p>
                    <p className="text-xs font-semibold text-slate-400">
                      {region.city ?? "Cidade não informada"} · {region.radiusMeters ? `${(region.radiusMeters / 1000).toFixed(1).replace(".", ",")} km de raio` : "Raio não definido"}
                    </p>
                  </button>
                  <div className="flex items-center gap-2">
                    <Badge value={region.active ? "Ativa" : "Pausada"} tone={region.active ? "green" : "neutral"} />
                    <button
                      onClick={() => toggleRegion(region)}
                      disabled={!!busyAction}
                      className="rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-black text-slate-700 hover:bg-slate-200 disabled:opacity-50"
                    >
                      {region.active ? "Pausar" : "Ativar"}
                    </button>
                    <button
                      onClick={() => deleteRegion(region)}
                      disabled={!!busyAction}
                      className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-black text-slate-500 hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                    >
                      {busyAction === `delete-region-${region.id}` ? "..." : "Excluir"}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Coluna direita: formulario */}
          <div className="self-start">
            <div className={classNames(
              "rounded-3xl border p-5",
              editingRegionId ? "border-violet-200 bg-violet-50" : "border-slate-200 bg-slate-50"
            )}>
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-black text-slate-900">
                  {editingRegionId ? "Editando região" : "Nova região"}
                </h3>
                {editingRegionId && (
                  <button onClick={resetRegionForm} className="text-xs font-black text-slate-500 hover:text-slate-700">
                    + Nova
                  </button>
                )}
              </div>

              <div className="grid gap-4">
                <Field label="Nome da região" value={regionForm.name} onChange={(v) => setRegionForm({ ...regionForm, name: v })} placeholder="Ex: Centro, Zona Sul..." />
                <Field label="Cidade" value={regionForm.city} onChange={(v) => setRegionForm({ ...regionForm, city: v })} />

                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <span className="text-sm font-black text-slate-600">Raio de atendimento</span>
                    <span className="rounded-xl bg-white px-3 py-1 text-sm font-black text-violet-700 shadow-sm">
                      {(regionForm.radiusMeters / 1000).toFixed(1).replace(".", ",")} km
                    </span>
                  </div>
                  <input
                    type="range" min={1000} max={30000} step={500}
                    value={regionForm.radiusMeters}
                    onChange={(e) => setRegionForm({ ...regionForm, radiusMeters: Number(e.target.value) })}
                    className="w-full accent-violet-600"
                  />
                  <div className="flex justify-between text-xs font-semibold text-slate-400">
                    <span>1 km</span><span>30 km</span>
                  </div>
                </div>

                <Toggle
                  checked={regionForm.active}
                  onChange={(v) => setRegionForm({ ...regionForm, active: v })}
                  label="Região ativa no app"
                />

                <details className="rounded-2xl border border-slate-200 bg-white">
                  <summary className="cursor-pointer px-4 py-3 text-xs font-black text-slate-500">
                    Ajuste fino de coordenadas
                  </summary>
                  <div className="grid grid-cols-2 gap-3 p-4 pt-0">
                    <Field label="Latitude" value={String(regionForm.centerLat)} onChange={(v) => setRegionForm({ ...regionForm, centerLat: Number(v) || 0 })} />
                    <Field label="Longitude" value={String(regionForm.centerLng)} onChange={(v) => setRegionForm({ ...regionForm, centerLng: Number(v) || 0 })} />
                  </div>
                </details>
              </div>

              <div className="mt-5 grid grid-cols-2 gap-3">
                <button
                  onClick={saveRegion}
                  disabled={busyAction === "region"}
                  className="rounded-2xl bg-slate-900 px-4 py-3 font-black text-white disabled:opacity-60"
                >
                  {busyAction === "region" ? "Salvando..." : editingRegionId ? "Salvar alterações" : "Criar região"}
                </button>
                <button
                  onClick={resetRegionForm}
                  className="rounded-2xl border border-slate-200 bg-white px-4 py-3 font-black text-slate-700"
                >
                  Cancelar
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── Tarifas ── */}
      {tab === "pricing" && (
        <div className="grid gap-6 xl:grid-cols-[1fr_1.1fr]">
          {/* Coluna esquerda: formulario */}
          <div className="grid gap-4 self-start rounded-3xl border border-slate-200 bg-slate-50 p-5">
            <h3 className="font-black text-slate-900">Nova tarifa</h3>

            <div className="grid gap-3">
              <Field label="Nome da tarifa" value={pricingForm.name} onChange={(v) => setPricingForm({ ...pricingForm, name: v })} placeholder="Ex: Tarifa noturna, Final de semana..." />
              <label className="grid gap-1">
                <span className="text-sm font-black text-slate-600">Aplicar em</span>
                <select
                  value={pricingForm.regionId}
                  onChange={(e) => setPricingForm({ ...pricingForm, regionId: e.target.value })}
                  className="rounded-2xl border border-slate-200 bg-white px-3 py-3 font-semibold text-slate-900 outline-none focus:border-violet-600"
                >
                  <option value="">Global (todas as regiões)</option>
                  {regions.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              </label>
            </div>

            <div className="h-px bg-slate-200" />
            <p className="text-xs font-black uppercase tracking-widest text-slate-400">Valores</p>

            <div className="grid grid-cols-2 gap-3">
              <MoneyField label="Bandeirada" hint="Valor inicial da corrida" value={pricingForm.baseFare} onChange={(v) => setPricingForm({ ...pricingForm, baseFare: v })} />
              <MoneyField label="Por quilômetro" hint="R$ por km rodado" value={pricingForm.perKm} onChange={(v) => setPricingForm({ ...pricingForm, perKm: v })} />
              <MoneyField label="Por minuto" hint="R$ por min de viagem" value={pricingForm.perMinute} onChange={(v) => setPricingForm({ ...pricingForm, perMinute: v })} />
              <MoneyField label="Mínimo cobrado" hint="Tarifa mínima garantida" value={pricingForm.minimum} onChange={(v) => setPricingForm({ ...pricingForm, minimum: v })} />
              <MoneyField label="Taxa de reserva" hint="Cobrada ao aceitar" value={pricingForm.bookingFee} onChange={(v) => setPricingForm({ ...pricingForm, bookingFee: v })} />
              <label className="grid gap-1">
                <span className="text-sm font-black text-slate-600">Multiplicador surge</span>
                <span className="text-xs font-medium text-slate-400">1.0 = normal · 2.0 = dobro</span>
                <div className="flex items-center overflow-hidden rounded-2xl border border-slate-200 bg-white focus-within:border-violet-600">
                  <span className="select-none border-r border-slate-100 px-3 py-3 text-sm font-bold text-slate-400">×</span>
                  <input
                    value={pricingForm.surge}
                    onChange={(e) => setPricingForm({ ...pricingForm, surge: e.target.value })}
                    placeholder="1.0"
                    className="flex-1 bg-transparent px-3 py-3 font-semibold text-slate-900 outline-none"
                  />
                </div>
              </label>
            </div>

            <PricingPreview form={pricingForm} />

            <button
              onClick={savePricing}
              disabled={busyAction === "pricing"}
              className="w-full rounded-2xl bg-violet-700 px-4 py-3.5 font-black text-white hover:bg-violet-800 disabled:opacity-60"
            >
              {busyAction === "pricing" ? "Salvando..." : "Salvar e ativar tarifa"}
            </button>
          </div>

          {/* Coluna direita: tarifas agrupadas por regiao */}
          <PricingGroupedList
            pricing={pricing}
            regions={regions}
            busyAction={busyAction}
            onActivate={activatePricing}
            onDeactivate={deactivatePricing}
            onDelete={deletePricing}
          />
        </div>
      )}
    </SectionCard>
  );
}

function PricingGroupedList({
  pricing,
  regions,
  busyAction,
  onActivate,
  onDeactivate,
  onDelete,
}: {
  pricing: PricingConfig[];
  regions: OperationRegion[];
  busyAction: string | null;
  onActivate: (item: PricingConfig) => void;
  onDeactivate: (item: PricingConfig) => void;
  onDelete: (item: PricingConfig) => void;
}) {
  // Agrupa tarifas: Global primeiro, depois por regiao
  const groups: Array<{ key: string; label: string; items: PricingConfig[]; hasActive: boolean; regionActive: boolean }> = [];

  const global = pricing.filter((p) => !p.regionId);
  if (global.length > 0 || pricing.length === 0) {
    groups.push({
      key: "global",
      label: "Global",
      items: global,
      hasActive: global.some((p) => p.isActive),
      regionActive: true,
    });
  }

  const byRegion = new Map<string, PricingConfig[]>();
  for (const p of pricing.filter((p) => p.regionId)) {
    const list = byRegion.get(p.regionId!) ?? [];
    list.push(p);
    byRegion.set(p.regionId!, list);
  }

  // Inclui tambem regioes sem nenhuma tarifa (para mostrar alerta)
  const allRegionIds = new Set([...byRegion.keys(), ...regions.map((r) => r.id)]);
  for (const regionId of allRegionIds) {
    const items = byRegion.get(regionId) ?? [];
    const region = regions.find((r) => r.id === regionId);
    if (!region) continue;
    groups.push({
      key: regionId,
      label: region.name,
      items,
      hasActive: items.some((p) => p.isActive),
      regionActive: region.active,
    });
  }

  if (groups.length === 0) {
    return (
      <p className="rounded-2xl border border-dashed border-slate-200 p-8 text-center text-sm font-bold text-slate-400">
        Nenhuma tarifa cadastrada ainda. Crie a primeira ao lado.
      </p>
    );
  }

  return (
    <div className="grid gap-5 self-start">
      <h3 className="text-xs font-black uppercase tracking-widest text-slate-400">Tarifas por região</h3>
      {groups.map((group) => (
        <div key={group.key} className="rounded-3xl border border-slate-200 bg-white overflow-hidden">
          {/* Header do grupo */}
          <div className={classNames(
            "flex items-center justify-between gap-3 px-5 py-3 border-b",
            !group.hasActive && group.regionActive ? "bg-amber-50 border-amber-200" : "bg-slate-50 border-slate-200"
          )}>
            <div className="flex items-center gap-2">
              <div className={classNames(
                "h-2 w-2 rounded-full",
                group.hasActive ? "bg-violet-500" : "bg-amber-400"
              )} />
              <span className="font-black text-slate-800">{group.label}</span>
              <span className="text-xs font-semibold text-slate-400">{group.items.length} tarifa{group.items.length !== 1 ? "s" : ""}</span>
            </div>
            {!group.hasActive && group.regionActive && (
              <span className="rounded-xl bg-amber-100 px-2.5 py-1 text-xs font-black text-amber-700">
                Sem tarifa ativa — corridas bloqueadas
              </span>
            )}
            {group.hasActive && (
              <span className="rounded-xl bg-violet-100 px-2.5 py-1 text-xs font-black text-violet-700">
                1 ativa
              </span>
            )}
          </div>

          {/* Lista de tarifas do grupo */}
          {group.items.length === 0 ? (
            <p className="px-5 py-4 text-sm font-semibold text-slate-400">Nenhuma tarifa nesta região.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {[...group.items].sort((a, b) => (b.isActive ? 1 : 0) - (a.isActive ? 1 : 0)).map((item) => (
                <div key={item.id} className={classNames("p-4", item.isActive && "bg-violet-50/50")}>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-black text-slate-900">{item.name ?? "Tarifa sem nome"}</p>
                        {item.isActive && <Badge value="Ativa" tone="green" />}
                      </div>
                      <p className="mt-0.5 text-xs font-semibold text-slate-400">{item.currency}</p>
                    </div>
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      {item.isActive ? (
                        <button
                          onClick={() => onDeactivate(item)}
                          disabled={busyAction === `deactivate-${item.id}`}
                          className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-black text-slate-600 hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                        >
                          {busyAction === `deactivate-${item.id}` ? "..." : "Desativar"}
                        </button>
                      ) : (
                        <>
                          <button
                            onClick={() => onActivate(item)}
                            disabled={busyAction === `pricing-${item.id}`}
                            className="rounded-xl bg-violet-600 px-3 py-1.5 text-xs font-black text-white hover:bg-violet-700 disabled:opacity-50"
                          >
                            {busyAction === `pricing-${item.id}` ? "..." : "Ativar"}
                          </button>
                          <button
                            onClick={() => onDelete(item)}
                            disabled={busyAction === `delete-${item.id}`}
                            className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-black text-slate-500 hover:border-red-200 hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                          >
                            {busyAction === `delete-${item.id}` ? "..." : "Excluir"}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="mt-3 grid grid-cols-3 gap-1.5">
                    {[
                      { label: "Bandeirada", value: money(item.baseFareCents) },
                      { label: "Por km", value: money(item.perKmCents) },
                      { label: "Por min", value: money(item.perMinuteCents) },
                      { label: "Minimo", value: money(item.minimumFareCents) },
                      { label: "Taxa", value: money(item.bookingFeeCents) },
                      { label: "Surge", value: `×${item.surgeMultiplier}` },
                    ].map(({ label, value }) => (
                      <div key={label} className={classNames("rounded-xl p-2 text-center", item.isActive ? "bg-white/80" : "bg-slate-50")}>
                        <p className="text-xs font-semibold text-slate-400">{label}</p>
                        <p className="text-sm font-black text-slate-800">{value}</p>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label className="grid gap-1 text-sm font-black text-slate-600">
      {label}
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="rounded-2xl border border-slate-200 bg-white px-3 py-3 font-semibold text-slate-900 outline-none focus:border-violet-600"
      />
    </label>
  );
}

function reaisToCents(value: string) {
  const normalized = Number(value.replace(",", "."));
  return Math.round((Number.isFinite(normalized) ? normalized : 0) * 100);
}

function PassengersPanel({
  passengers,
  authFetch,
  onChanged,
}: {
  passengers: Passenger[];
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<"ALL" | "ACTIVE" | "BLOCKED">("ALL");
  const [selected, setSelected] = useState<Passenger | null>(null);

  const query = search.trim().toLowerCase();
  const visible = passengers
    .filter((p) => filter === "ALL" || (filter === "BLOCKED" ? p.blocked : !p.blocked))
    .filter((p) =>
      !query ||
      (p.name ?? "").toLowerCase().includes(query) ||
      p.phone.includes(query) ||
      (p.email ?? "").toLowerCase().includes(query)
    );

  const blocked = passengers.filter((p) => p.blocked).length;
  const active = passengers.length - blocked;

  return (
    <SectionCard
      title="Passageiros"
      subtitle="Base de passageiros cadastrados."
      action={
        <div className="flex flex-wrap gap-2">
          {(["ALL", "ACTIVE", "BLOCKED"] as const).map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={classNames(
                "rounded-2xl px-4 py-2 text-sm font-black",
                filter === f ? "bg-slate-900 text-white" : "bg-slate-100 text-slate-600"
              )}
            >
              {f === "ALL" ? `Todos (${passengers.length})` : f === "ACTIVE" ? `Ativos (${active})` : `Bloqueados (${blocked})`}
            </button>
          ))}
        </div>
      }
    >
      <div className="relative mb-4">
        <svg className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por nome, telefone ou e-mail..."
          className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-10 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500 focus:bg-white"
        />
        {search && (
          <button onClick={() => setSearch("")} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700">✕</button>
        )}
      </div>

      <div className="overflow-x-auto rounded-2xl border border-slate-200">
        <table className="w-full min-w-[560px] text-left text-sm">
          <thead>
            <tr className="bg-slate-50 text-xs font-black uppercase tracking-wide text-slate-400">
              <th className="px-4 py-3">Passageiro</th>
              <th className="px-4 py-3">Contato</th>
              <th className="px-4 py-3">Corridas</th>
              <th className="px-4 py-3">Chamados</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Cadastro</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {visible.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center font-bold text-slate-400">
                  {query ? `Nenhum passageiro encontrado para "${search}".` : "Nenhum passageiro nesta lista."}
                </td>
              </tr>
            )}
            {visible.map((p) => (
              <tr key={p.id} className={classNames("border-t border-slate-100 transition hover:bg-slate-50", p.blocked && "bg-red-50/40")}>
                <td className="px-4 py-3">
                  <p className="font-bold text-slate-900">{p.name ?? "—"}</p>
                </td>
                <td className="px-4 py-3">
                  <p className="text-xs font-semibold text-slate-700">{p.phone}</p>
                  {p.email && <p className="text-xs font-medium text-slate-400">{p.email}</p>}
                </td>
                <td className="px-4 py-3">
                  <span className="rounded-xl bg-slate-100 px-2.5 py-1 text-xs font-black text-slate-700">{p.totalRides}</span>
                </td>
                <td className="px-4 py-3">
                  {p.totalTickets > 0 ? (
                    <span className="rounded-xl bg-amber-100 px-2.5 py-1 text-xs font-black text-amber-700">{p.totalTickets}</span>
                  ) : (
                    <span className="text-xs font-semibold text-slate-300">0</span>
                  )}
                </td>
                <td className="px-4 py-3">
                  {p.blocked ? <Badge value="Bloqueado" tone="red" /> : <Badge value="Ativo" tone="green" />}
                </td>
                <td className="px-4 py-3 text-xs font-medium text-slate-400">{dateTime(p.createdAt)}</td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => setSelected(p)}
                    className="rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-black text-white"
                  >
                    Detalhes
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {visible.length > 0 && (
        <p className="mt-2 text-xs font-bold text-slate-400">
          {visible.length}{query || filter !== "ALL" ? ` de ${passengers.length}` : ""} passageiro{passengers.length !== 1 ? "s" : ""}
        </p>
      )}

      {selected && (
        <PassengerDetailModal
          passenger={selected}
          authFetch={authFetch}
          onChanged={() => { onChanged(); setSelected(null); }}
          onClose={() => setSelected(null)}
        />
      )}
    </SectionCard>
  );
}

function PassengerDetailModal({
  passenger,
  authFetch,
  onChanged,
  onClose,
}: {
  passenger: Passenger;
  authFetch: AuthFetch;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  const toggleBlock = async () => {
    const action = passenger.blocked ? "unblock" : "block";
    if (!window.confirm(passenger.blocked ? "Desbloquear a conta deste passageiro?" : "Bloquear a conta deste passageiro? Ele não conseguirá fazer login.")) return;
    setBusy(true);
    setMessage("");
    try {
      await authFetch(`/admin/users/${passenger.id}/${action}`, { method: "PATCH" });
      onChanged();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Erro ao atualizar conta");
      setBusy(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-3xl bg-white shadow-2xl">
        <div className="flex items-start justify-between gap-3 border-b border-slate-100 px-6 py-5">
          <div>
            <h2 className="text-xl font-black">{passenger.name ?? "Sem nome"}</h2>
            <p className="text-sm font-semibold text-slate-500">{passenger.phone}</p>
          </div>
          <div className="flex items-center gap-2">
            {passenger.blocked ? <Badge value="Bloqueado" tone="red" /> : <Badge value="Ativo" tone="green" />}
            <button onClick={onClose} className="rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-black text-slate-600 hover:bg-slate-200">Fechar</button>
          </div>
        </div>

        <div className="grid gap-5 px-6 py-5">
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Dados</p>
            <div className="grid grid-cols-2 gap-3">
              <DetailField label="Telefone" value={passenger.phone} />
              <DetailField label="E-mail" value={passenger.email ?? "—"} />
              <DetailField label="Cadastro" value={dateTime(passenger.createdAt) ?? "—"} />
              {passenger.blockedAt && <DetailField label="Bloqueado em" value={dateTime(passenger.blockedAt) ?? "—"} />}
            </div>
          </div>

          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Atividade</p>
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-2xl bg-slate-50 px-4 py-3 text-center">
                <p className="text-xs font-bold text-slate-400">Corridas</p>
                <p className="mt-0.5 text-2xl font-black text-slate-900">{passenger.totalRides}</p>
              </div>
              <div className="rounded-2xl bg-slate-50 px-4 py-3 text-center">
                <p className="text-xs font-bold text-slate-400">Chamados</p>
                <p className={classNames("mt-0.5 text-2xl font-black", passenger.totalTickets > 0 ? "text-amber-600" : "text-slate-900")}>
                  {passenger.totalTickets}
                </p>
              </div>
            </div>
          </div>

          {message && (
            <p className="rounded-2xl bg-red-50 p-3 text-sm font-bold text-red-700">{message}</p>
          )}

          <div className="border-t border-slate-100 pt-4">
            <button
              onClick={toggleBlock}
              disabled={busy}
              className={classNames(
                "w-full rounded-2xl py-3 text-sm font-black disabled:opacity-60",
                passenger.blocked
                  ? "bg-emerald-600 text-white hover:bg-emerald-700"
                  : "border border-red-300 text-red-600 hover:bg-red-50"
              )}
            >
              {busy ? "Aguarde..." : passenger.blocked ? "Desbloquear conta" : "Bloquear conta"}
            </button>
            {passenger.blocked && (
              <p className="mt-1.5 text-center text-xs font-semibold text-red-500">
                Conta bloqueada — passageiro não consegue acessar o app
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

const TICKET_TYPE_LABEL: Record<TicketType, string> = {
  PAYMENT: "Pagamento",
  RIDE_CANCELLATION: "Cancelamento",
  SAFETY: "Segurança",
  APP_ISSUE: "Problema no App",
  OTHER: "Outro",
};

const TICKET_TYPE_STYLE: Record<TicketType, string> = {
  PAYMENT: "bg-amber-100 text-amber-800",
  RIDE_CANCELLATION: "bg-orange-100 text-orange-800",
  SAFETY: "bg-red-100 text-red-800",
  APP_ISSUE: "bg-blue-100 text-blue-700",
  OTHER: "bg-slate-100 text-slate-600",
};

const STATUS_TABS: { value: TicketStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "Todos" },
  { value: "OPEN", label: "Abertos" },
  { value: "IN_REVIEW", label: "Em análise" },
  { value: "RESOLVED", label: "Resolvidos" },
  { value: "CLOSED", label: "Fechados" },
];

function SupportPanel({
  tickets,
  authFetch,
  onChanged,
}: {
  tickets: SupportTicket[];
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [statusFilter, setStatusFilter] = useState<TicketStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<SupportTicket | null>(null);

  const query = search.trim().toLowerCase();
  const visible = tickets
    .filter((t) => statusFilter === "ALL" || t.status === statusFilter)
    .filter((t) =>
      !query ||
      t.subject.toLowerCase().includes(query) ||
      (t.creator?.name ?? "").toLowerCase().includes(query) ||
      t.creator?.phone.includes(query) ||
      TICKET_TYPE_LABEL[t.type].toLowerCase().includes(query)
    );

  const counts = (["OPEN", "IN_REVIEW", "RESOLVED", "CLOSED"] as TicketStatus[]).reduce(
    (acc, s) => ({ ...acc, [s]: tickets.filter((t) => t.status === s).length }),
    {} as Record<TicketStatus, number>
  );

  return (
    <SectionCard
      title="Suporte"
      subtitle="Chamados de passageiros e motoristas."
    >
      {/* Tabs de status com contadores */}
      <div className="mb-4 flex flex-wrap gap-2">
        {STATUS_TABS.map(({ value, label }) => {
          const count = value === "ALL" ? tickets.length : counts[value as TicketStatus];
          return (
            <button
              key={value}
              onClick={() => setStatusFilter(value)}
              className={classNames(
                "flex items-center gap-1.5 rounded-2xl px-4 py-2 text-sm font-black transition",
                statusFilter === value
                  ? "bg-slate-900 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              )}
            >
              {label}
              {count > 0 && (
                <span className={classNames(
                  "rounded-full px-1.5 py-0.5 text-xs",
                  statusFilter === value ? "bg-white/20" : "bg-slate-200 text-slate-700"
                )}>
                  {count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Busca */}
      <div className="relative mb-4">
        <svg
          className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
          width="16" height="16" viewBox="0 0 24 24" fill="none"
          stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
        >
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Buscar por assunto, nome ou tipo..."
          className="w-full rounded-2xl border border-slate-200 bg-slate-50 py-3 pl-10 pr-10 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500 focus:bg-white"
        />
        {search && (
          <button onClick={() => setSearch("")} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700">
            ✕
          </button>
        )}
      </div>

      {/* Lista */}
      <div className="grid gap-3 lg:grid-cols-2">
        {visible.length === 0 && (
          <p className="font-bold text-slate-400">
            {query ? `Nenhum chamado encontrado para "${search}".` : "Nenhum chamado nesta categoria."}
          </p>
        )}
        {visible.map((ticket) => (
          <TicketCard key={ticket.id} ticket={ticket} onOpen={() => setSelected(ticket)} />
        ))}
      </div>

      {query && visible.length > 0 && (
        <p className="mt-3 text-xs font-bold text-slate-400">
          {visible.length} de {tickets.length} chamado{tickets.length !== 1 ? "s" : ""}
        </p>
      )}

      {selected && (
        <TicketModal
          ticket={selected}
          authFetch={authFetch}
          onChanged={() => { onChanged(); setSelected(null); }}
          onClose={() => setSelected(null)}
        />
      )}
    </SectionCard>
  );
}

function TicketCard({ ticket, onOpen }: { ticket: SupportTicket; onOpen: () => void }) {
  const isSafety = ticket.type === "SAFETY";
  return (
    <div className={classNames(
      "flex flex-col gap-3 rounded-3xl border p-4 transition",
      isSafety ? "border-red-200 bg-red-50" : "border-slate-200 bg-slate-50"
    )}>
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-1.5 mb-1">
            <span className={classNames("rounded-lg px-2 py-0.5 text-xs font-black", TICKET_TYPE_STYLE[ticket.type])}>
              {isSafety && "⚠ "}{TICKET_TYPE_LABEL[ticket.type]}
            </span>
            <Badge value={statusLabel(ticket.status)} tone={statusTone(ticket.status)} />
          </div>
          <p className="font-black text-slate-900 leading-snug">{ticket.subject}</p>
          <p className="mt-0.5 text-xs font-semibold text-slate-500">
            {ticket.creator?.name ?? ticket.creator?.phone} · {dateTime(ticket.createdAt)}
          </p>
        </div>
        <button
          onClick={onOpen}
          className="shrink-0 rounded-2xl bg-slate-900 px-3 py-2 text-xs font-black text-white"
        >
          Abrir
        </button>
      </div>
      <p className="line-clamp-2 text-xs font-medium text-slate-500">{ticket.description}</p>
    </div>
  );
}

function TicketModal({
  ticket,
  authFetch,
  onChanged,
  onClose,
}: {
  ticket: SupportTicket;
  authFetch: AuthFetch;
  onChanged: () => void;
  onClose: () => void;
}) {
  const [resolution, setResolution] = useState(ticket.resolution ?? "");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [confirmAction, setConfirmAction] = useState<TicketStatus | null>(null);

  const update = async (status: TicketStatus, res?: string) => {
    if (status === "RESOLVED" && !res?.trim()) {
      setMessage("Descreva a resolução antes de marcar como resolvido.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      await authFetch(`/support/tickets/${ticket.id}`, {
        method: "PATCH",
        body: JSON.stringify({ status, ...(res !== undefined ? { resolution: res } : {}) }),
      });
      onChanged();
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Erro ao atualizar chamado");
      setBusy(false);
    }
  };

  const actions: { label: string; status: TicketStatus; style: string; needsResolution?: boolean }[] = (() => {
    switch (ticket.status) {
      case "OPEN":
        return [
          { label: "Iniciar análise", status: "IN_REVIEW", style: "bg-blue-600 text-white" },
          { label: "Fechar sem resolução", status: "CLOSED", style: "border border-slate-300 text-slate-700" },
        ];
      case "IN_REVIEW":
        return [
          { label: "Marcar como resolvido", status: "RESOLVED", style: "bg-emerald-600 text-white", needsResolution: true },
          { label: "Fechar sem resolução", status: "CLOSED", style: "border border-slate-300 text-slate-700" },
        ];
      case "RESOLVED":
      case "CLOSED":
        return [
          { label: "Reabrir chamado", status: "OPEN", style: "border border-amber-400 text-amber-700" },
        ];
      default:
        return [];
    }
  })();

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-3xl bg-white shadow-2xl">
        {/* Header */}
        <div className="border-b border-slate-100 px-6 py-5">
          <div className="flex items-start justify-between gap-3">
            <div className="flex flex-wrap gap-1.5 items-center">
              <span className={classNames("rounded-lg px-2 py-0.5 text-xs font-black", TICKET_TYPE_STYLE[ticket.type])}>
                {TICKET_TYPE_LABEL[ticket.type]}
              </span>
              <Badge value={statusLabel(ticket.status)} tone={statusTone(ticket.status)} />
            </div>
            <button onClick={onClose} className="shrink-0 rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-black text-slate-600 hover:bg-slate-200">
              Fechar
            </button>
          </div>
          <h2 className="mt-3 text-lg font-black leading-snug">{ticket.subject}</h2>
        </div>

        <div className="grid gap-5 px-6 py-5">
          {/* Descrição */}
          <div>
            <p className="mb-1.5 text-xs font-black uppercase tracking-widest text-slate-400">Descrição</p>
            <p className="text-sm font-medium text-slate-700 leading-relaxed">{ticket.description}</p>
          </div>

          {/* Criador */}
          <div>
            <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Criador</p>
            <div className="grid grid-cols-2 gap-3">
              <DetailField label="Nome" value={ticket.creator?.name ?? "—"} />
              <DetailField label="Telefone" value={ticket.creator?.phone ?? "—"} />
              {ticket.creator?.role && <DetailField label="Perfil" value={ticket.creator.role === "DRIVER" ? "Motorista" : ticket.creator.role === "PASSENGER" ? "Passageiro" : "Admin"} />}
            </div>
          </div>

          {/* Corrida vinculada */}
          {ticket.ride && (
            <div>
              <p className="mb-2 text-xs font-black uppercase tracking-widest text-slate-400">Corrida vinculada</p>
              <div className="grid grid-cols-2 gap-3">
                <DetailField label="Status" value={statusLabel(ticket.ride.status)} />
                <DetailField label="Pagamento" value={statusLabel(ticket.ride.paymentStatus)} />
                {ticket.ride.originAddress && <DetailField label="Origem" value={ticket.ride.originAddress} />}
                {ticket.ride.destinationAddress && <DetailField label="Destino" value={ticket.ride.destinationAddress} />}
              </div>
            </div>
          )}

          {/* Datas */}
          <div className="grid grid-cols-2 gap-3">
            <DetailField label="Aberto em" value={dateTime(ticket.createdAt) ?? "—"} />
            {ticket.closedAt && <DetailField label="Fechado em" value={dateTime(ticket.closedAt) ?? "—"} />}
          </div>

          {/* Resolução */}
          {(ticket.status === "IN_REVIEW" || ticket.status === "OPEN" || ticket.resolution) && (
            <div>
              <p className="mb-1.5 text-xs font-black uppercase tracking-widest text-slate-400">
                Resolução {ticket.status === "IN_REVIEW" ? "(obrigatória para resolver)" : ""}
              </p>
              {ticket.status === "RESOLVED" || ticket.status === "CLOSED" ? (
                <p className="text-sm font-medium text-slate-700 leading-relaxed">
                  {ticket.resolution ?? "Nenhuma resolução registrada."}
                </p>
              ) : (
                <textarea
                  value={resolution}
                  onChange={(e) => setResolution(e.target.value)}
                  placeholder="Descreva como o chamado foi resolvido..."
                  className="w-full min-h-24 rounded-2xl border border-slate-200 p-3 text-sm font-semibold text-slate-800 outline-none focus:border-violet-500"
                />
              )}
            </div>
          )}

          {/* Feedback de erro */}
          {message && (
            <p className="rounded-2xl bg-red-50 p-3 text-sm font-bold text-red-700">{message}</p>
          )}

          {/* Ações */}
          {actions.length > 0 && !confirmAction && (
            <div className={classNames("grid gap-2 border-t border-slate-100 pt-4", actions.length > 1 ? "grid-cols-2" : "grid-cols-1")}>
              {actions.map((action) => (
                <button
                  key={action.status}
                  disabled={busy}
                  onClick={() => {
                    if (action.needsResolution) {
                      update(action.status, resolution);
                    } else {
                      setConfirmAction(action.status);
                    }
                  }}
                  className={classNames("rounded-2xl py-3 text-sm font-black disabled:opacity-60", action.style)}
                >
                  {action.label}
                </button>
              ))}
            </div>
          )}

          {confirmAction && (
            <div className="grid gap-3 border-t border-slate-100 pt-4">
              <p className="text-sm font-bold text-slate-700">
                Confirmar: <span className="font-black">{statusLabel(confirmAction)}</span>?
              </p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => { update(confirmAction); setConfirmAction(null); }}
                  disabled={busy}
                  className="rounded-2xl bg-slate-900 py-3 text-sm font-black text-white disabled:opacity-60"
                >
                  Confirmar
                </button>
                <button
                  onClick={() => setConfirmAction(null)}
                  className="rounded-2xl bg-slate-100 py-3 text-sm font-black text-slate-700"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function FinancialPanel({
  financial,
  paymentRequests,
  systemConfig,
  authFetch,
  onChanged,
}: {
  financial: FinancialSummary | null;
  paymentRequests: PaymentRequest[];
  systemConfig: SystemConfig;
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [settleTarget, setSettleTarget] = useState<DriverFinancialRow | null>(null);
  const [amount, setAmount] = useState("");
  const [notes, setNotes] = useState("");
  const [method, setMethod] = useState("PIX");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);

  const overview = financial?.overview;
  const byDriver = financial?.byDriver ?? [];
  const recentSettlements = financial?.recentSettlements ?? [];

  const openSettle = (row: DriverFinancialRow) => {
    setSettleTarget(row);
    setAmount(row.balanceCents > 0 ? (row.balanceCents / 100).toFixed(2).replace(".", ",") : "");
    setNotes("");
    setMethod("PIX");
    setErr("");
  };

  const submitSettle = async () => {
    if (!settleTarget) return;
    const cents = Math.round(parseFloat(amount.replace(",", ".")) * 100);
    if (!cents || cents <= 0) { setErr("Informe um valor válido"); return; }
    setBusy(true);
    setErr("");
    try {
      await authFetch(`/admin/financial/drivers/${settleTarget.driverId}/settle`, {
        method: "POST",
        body: JSON.stringify({ amountCents: cents, notes: notes || null, method }),
      });
      setSettleTarget(null);
      onChanged();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao registrar");
    } finally {
      setBusy(false);
    }
  };

  const doDelete = async (id: string) => {
    try {
      await authFetch(`/admin/financial/settlements/${id}`, { method: "DELETE" });
      setDeleteConfirm(null);
      onChanged();
    } catch {
      setDeleteConfirm(null);
    }
  };

  // Map driverId → pending payment request (for inline indicator)
  const pendingByDriver = new Map(
    paymentRequests.filter((r) => r.status === "PENDING").map((r) => [r.driverId, r])
  );

  // % of fees covered (all drivers)
  const coveragePct = overview && overview.totalPlatformFeeCents > 0
    ? Math.min(100, Math.round((overview.totalSettledCents / overview.totalPlatformFeeCents) * 100))
    : 0;

  return (
    <div className="grid gap-5">
      {/* Header */}
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-2xl font-black text-slate-950">Cobranças de plataforma</h2>
          <p className="mt-1 text-sm font-semibold text-slate-500">
            Taxa de uso por corrida — liquidações e solicitações dos motoristas.
          </p>
        </div>
        {overview && (
          <div className="flex items-center gap-2 rounded-2xl bg-slate-100 px-4 py-2">
            <span className="text-xs font-black uppercase tracking-widest text-slate-400">Cobertura</span>
            <span className={classNames(
              "text-lg font-black",
              coveragePct >= 80 ? "text-emerald-600" : coveragePct >= 40 ? "text-amber-600" : "text-red-600"
            )}>{coveragePct}%</span>
            <span className="text-xs text-slate-400">liquidado</span>
          </div>
        )}
      </div>

      {/* Overview cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          label="Taxa total acumulada"
          value={money(overview?.totalPlatformFeeCents)}
          hint={`${overview?.totalRides ?? 0} corridas finalizadas`}
        />
        <MetricCard
          label="Total confirmado"
          value={money(overview?.totalSettledCents)}
          hint="Pagamentos confirmados pelo admin"
        />
        <MetricCard
          label="Saldo em aberto"
          value={money(overview?.totalPendingCents)}
          hint="A receber dos motoristas"
        />
        <MetricCard
          label="Repasse líquido"
          value={money(overview?.totalDriverReceivableCents)}
          hint="Valor total a receber pelos motoristas"
        />
      </div>

      {/* Pending payment requests — most urgent, shown first */}
      <PaymentRequestsPanel requests={paymentRequests} authFetch={authFetch} onChanged={onChanged} />

      {/* Per-driver table */}
      <SectionCard
        title="Saldo por motorista"
        subtitle="Taxa acumulada, pagamentos confirmados e saldo devedor. Ordenado por saldo pendente."
      >
        {byDriver.length === 0 ? (
          <p className="py-10 text-center text-sm font-semibold text-slate-400">
            Nenhum motorista com corridas finalizadas ainda.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-black uppercase tracking-widest text-slate-400">
                  <th className="pb-3 pr-4">Motorista</th>
                  <th className="pb-3 pr-4 text-right">Corridas</th>
                  <th className="pb-3 pr-4 text-right">Taxa plataforma</th>
                  <th className="pb-3 pr-4 text-right">Confirmado</th>
                  <th className="pb-3 pr-4 text-right">Saldo devedor</th>
                  <th className="pb-3 pr-4">Situação</th>
                  <th className="pb-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {byDriver.map((row) => {
                  const pendingReq = pendingByDriver.get(row.driverId);
                  const debtLimit = parseInt(systemConfig.DRIVER_DEBT_LIMIT_CENTS ?? "5000");
                  const isOverLimit = row.balanceCents > debtLimit;
                  const isPaid = row.balanceCents === 0 && row.feeCents > 0;
                  const hasPendingReq = !!pendingReq;

                  const balanceTone = isPaid
                    ? "text-emerald-600"
                    : isOverLimit
                    ? "text-red-600 font-black"
                    : row.balanceCents > 0
                    ? "text-amber-600"
                    : "text-slate-400";

                  const rowBg = isOverLimit
                    ? "bg-red-50/40"
                    : hasPendingReq
                    ? "bg-blue-50/40"
                    : isPaid
                    ? "bg-emerald-50/30"
                    : "";

                  const coverFrac = row.feeCents > 0
                    ? Math.min(1, row.settledCents / row.feeCents)
                    : 1;

                  return (
                    <tr key={row.driverId} className={classNames("group transition", rowBg, "hover:brightness-95")}>
                      <td className="py-3 pr-4">
                        <p className="font-bold text-slate-800">{row.driverName ?? "—"}</p>
                        <p className="text-xs text-slate-400">{row.driverPhone ?? "—"}</p>
                      </td>
                      <td className="py-3 pr-4 text-right font-semibold text-slate-600">{row.rides}</td>
                      <td className="py-3 pr-4 text-right font-semibold text-slate-700">{money(row.feeCents)}</td>
                      <td className="py-3 pr-4 text-right">
                        <div className="inline-flex flex-col items-end gap-1">
                          <span className="font-semibold text-emerald-600">{money(row.settledCents)}</span>
                          {row.feeCents > 0 && (
                            <div className="h-1 w-16 overflow-hidden rounded-full bg-slate-200">
                              <div
                                className={classNames(
                                  "h-full rounded-full transition-all",
                                  coverFrac >= 1 ? "bg-emerald-500" : coverFrac > 0.5 ? "bg-amber-400" : "bg-red-400"
                                )}
                                style={{ width: `${Math.round(coverFrac * 100)}%` }}
                              />
                            </div>
                          )}
                        </div>
                      </td>
                      <td className={classNames("py-3 pr-4 text-right text-base", balanceTone)}>
                        {money(row.balanceCents)}
                      </td>
                      <td className="py-3 pr-4">
                        {isPaid ? (
                          <Badge value="Em dia" tone="green" />
                        ) : isOverLimit ? (
                          <Badge value="Bloqueado" tone="red" />
                        ) : hasPendingReq ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-black text-blue-700">
                            <span className="relative flex h-1.5 w-1.5">
                              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-blue-400 opacity-75" />
                              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-blue-500" />
                            </span>
                            Pagamento enviado
                          </span>
                        ) : row.balanceCents > 0 ? (
                          <Badge value="Pendente" tone="yellow" />
                        ) : (
                          <Badge value="Sem corridas" tone="neutral" />
                        )}
                      </td>
                      <td className="py-3 text-right">
                        {row.feeCents > 0 && !hasPendingReq && (
                          <button
                            onClick={() => openSettle(row)}
                            className="rounded-xl bg-violet-700 px-3 py-1.5 text-xs font-black text-white hover:bg-violet-800"
                          >
                            Registrar pagamento
                          </button>
                        )}
                        {hasPendingReq && (
                          <span className="text-xs font-semibold text-blue-500">
                            Aguardando confirmação
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>

      {/* Settlement history */}
      {recentSettlements.length > 0 && (
        <SectionCard
          title="Histórico de liquidações"
          subtitle={`Últimas ${recentSettlements.length} liquidações registradas`}
        >
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-black uppercase tracking-widest text-slate-400">
                  <th className="pb-3 pr-4">Motorista</th>
                  <th className="pb-3 pr-4 text-right">Valor</th>
                  <th className="pb-3 pr-4">Origem</th>
                  <th className="pb-3 pr-4">Método</th>
                  <th className="pb-3 pr-4 max-w-[160px]">Observações</th>
                  <th className="pb-3 pr-4 whitespace-nowrap">Data</th>
                  <th className="pb-3 pr-4">Admin</th>
                  <th className="pb-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {recentSettlements.map((st) => (
                  <tr key={st.id} className="group hover:bg-slate-50">
                    <td className="py-3 pr-4">
                      <p className="font-bold text-slate-800">{st.driverName ?? "—"}</p>
                    </td>
                    <td className="py-3 pr-4 text-right font-black text-emerald-600">{money(st.amountCents)}</td>
                    <td className="py-3 pr-4">
                      {st.paymentRequestId ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-violet-50 px-2 py-0.5 text-xs font-black text-violet-700">
                          Motorista
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-black text-slate-500">
                          Manual
                        </span>
                      )}
                    </td>
                    <td className="py-3 pr-4">
                      <Badge
                        value={st.method === "PIX" ? "PIX" : st.method === "CASH" ? "Dinheiro" : "Transferência"}
                        tone={st.method === "PIX" ? "blue" : "neutral"}
                      />
                    </td>
                    <td className="py-3 pr-4 max-w-[160px] truncate text-slate-500">{st.notes ?? "—"}</td>
                    <td className="py-3 pr-4 text-slate-500 whitespace-nowrap">{dateTime(st.settledAt)}</td>
                    <td className="py-3 pr-4 text-slate-500 text-xs">{st.settledBy}</td>
                    <td className="py-3 text-right">
                      {deleteConfirm === st.id ? (
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => doDelete(st.id)}
                            className="rounded-xl bg-red-600 px-3 py-1.5 text-xs font-black text-white hover:bg-red-700"
                          >
                            Confirmar exclusão
                          </button>
                          <button
                            onClick={() => setDeleteConfirm(null)}
                            className="rounded-xl bg-slate-100 px-3 py-1.5 text-xs font-black text-slate-700"
                          >
                            Cancelar
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => setDeleteConfirm(st.id)}
                          className="rounded-xl bg-red-50 px-3 py-1.5 text-xs font-black text-red-600 opacity-0 transition hover:bg-red-100 group-hover:opacity-100"
                        >
                          Excluir
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </SectionCard>
      )}

      {/* System config */}
      <SystemConfigPanel config={systemConfig} authFetch={authFetch} onChanged={onChanged} />

      {/* Register settlement modal */}
      {settleTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-md overflow-hidden rounded-3xl bg-white shadow-2xl">
            {/* Modal header */}
            <div className="bg-slate-950 px-6 py-5">
              <p className="text-xs font-black uppercase tracking-[0.25em] text-violet-300">Liquidação manual</p>
              <h3 className="mt-1 text-xl font-black text-white">Registrar pagamento</h3>
              <p className="mt-0.5 text-sm font-semibold text-slate-400">{settleTarget.driverName ?? "Motorista"}</p>
            </div>

            <div className="p-6">
              {/* Driver balance summary */}
              <div className="mb-5 grid grid-cols-3 gap-3 rounded-2xl bg-slate-50 p-4 text-center">
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-400">Taxa total</p>
                  <p className="mt-1 font-black text-slate-700">{money(settleTarget.feeCents)}</p>
                </div>
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-400">Liquidado</p>
                  <p className="mt-1 font-black text-emerald-600">{money(settleTarget.settledCents)}</p>
                </div>
                <div>
                  <p className="text-xs font-black uppercase tracking-widest text-slate-400">Saldo</p>
                  <p className="mt-1 font-black text-red-600">{money(settleTarget.balanceCents)}</p>
                </div>
              </div>

              {/* Amount */}
              <label className="block text-sm font-black text-slate-700">Valor recebido (R$)</label>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0,00"
                className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 font-bold text-slate-800 outline-none focus:border-violet-500"
              />

              {/* Method selector */}
              <label className="mt-4 block text-sm font-black text-slate-700">Forma de pagamento</label>
              <div className="mt-2 flex gap-2">
                {(["PIX", "CASH", "TRANSFER"] as const).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setMethod(m)}
                    className={classNames(
                      "flex-1 rounded-2xl py-2.5 text-sm font-black transition",
                      method === m ? "bg-violet-700 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200",
                    )}
                  >
                    {m === "PIX" ? "PIX" : m === "CASH" ? "Dinheiro" : "Transferência"}
                  </button>
                ))}
              </div>

              {/* Notes */}
              <label className="mt-4 block text-sm font-black text-slate-700">Observações (opcional)</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ex: Pagamento referente ao mês de maio..."
                className="mt-2 w-full min-h-20 rounded-2xl border border-slate-200 p-3 text-sm font-semibold text-slate-700 outline-none focus:border-violet-500"
              />

              {err && (
                <p className="mt-3 rounded-2xl bg-red-50 p-3 text-sm font-bold text-red-700">{err}</p>
              )}

              {/* Actions */}
              <div className="mt-5 grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setSettleTarget(null)}
                  className="rounded-2xl bg-slate-100 py-3 text-sm font-black text-slate-700 hover:bg-slate-200"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={submitSettle}
                  disabled={busy}
                  className="rounded-2xl bg-violet-700 py-3 text-sm font-black text-white hover:bg-violet-800 disabled:opacity-60"
                >
                  {busy ? "Registrando..." : "Confirmar pagamento"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function PaymentRequestsPanel({
  requests,
  authFetch,
  onChanged,
}: {
  requests: PaymentRequest[];
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [busy, setBusy] = useState<string | null>(null);
  const [rejectTarget, setRejectTarget] = useState<PaymentRequest | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [err, setErr] = useState("");

  const pending = requests.filter((r) => r.status === "PENDING");
  const done = requests.filter((r) => r.status !== "PENDING");

  const confirm = async (req: PaymentRequest) => {
    setBusy(req.id);
    setErr("");
    try {
      await authFetch(`/admin/financial/payment-requests/${req.id}/confirm`, { method: "PATCH" });
      onChanged();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao confirmar");
    } finally {
      setBusy(null);
    }
  };

  const reject = async () => {
    if (!rejectTarget) return;
    setBusy(rejectTarget.id);
    setErr("");
    try {
      await authFetch(`/admin/financial/payment-requests/${rejectTarget.id}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ reason: rejectReason || "Rejeitado pelo administrador" }),
      });
      setRejectTarget(null);
      setRejectReason("");
      onChanged();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Erro ao rejeitar");
    } finally {
      setBusy(null);
    }
  };

  if (requests.length === 0) return null;

  return (
    <>
      <SectionCard
        title="Solicitações de pagamento"
        subtitle="Motoristas que informaram ter realizado o PIX para a plataforma"
        action={
          pending.length > 0 ? (
            <span className="flex items-center gap-1.5 rounded-full bg-red-100 px-3 py-1 text-xs font-black text-red-700">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-red-400 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-red-500" />
              </span>
              {pending.length} pendente{pending.length > 1 ? "s" : ""}
            </span>
          ) : undefined
        }
      >
        {err && <p className="mb-4 rounded-2xl bg-red-50 p-3 text-sm font-bold text-red-700">{err}</p>}

        {pending.length > 0 && (
          <div className="mb-6 grid gap-3">
            {pending.map((req) => (
              <div
                key={req.id}
                className="flex flex-col gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-black text-slate-800">{req.driver.name ?? "—"}</p>
                    <Badge value="Pendente" tone="yellow" />
                  </div>
                  <p className="text-xs text-slate-500">{req.driver.phone}</p>
                  <p className="mt-1 text-sm font-semibold text-slate-700">
                    Valor informado: <span className="font-black text-emerald-700">{money(req.amountCents)}</span>
                  </p>
                  {req.notes && <p className="mt-0.5 text-xs text-slate-500">{req.notes}</p>}
                  {req.receiptUrl ? (
                    <a
                      href={`${API_URL}${req.receiptUrl}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-1 inline-flex items-center gap-1 text-xs font-black text-violet-700 hover:underline"
                    >
                      <svg className="h-3.5 w-3.5" viewBox="0 0 20 20" fill="currentColor"><path fillRule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 5.5 2-3.5 3 6z" clipRule="evenodd" /></svg>
                      Ver comprovante
                    </a>
                  ) : (
                    <p className="mt-0.5 text-xs italic text-slate-400">Sem comprovante anexado</p>
                  )}
                  <p className="mt-0.5 text-xs text-slate-400">Solicitado {dateTime(req.requestedAt)}</p>
                </div>
                <div className="flex shrink-0 gap-2">
                  <button
                    onClick={() => { setRejectTarget(req); setRejectReason(""); setErr(""); }}
                    disabled={busy === req.id}
                    className="rounded-xl bg-red-100 px-4 py-2 text-sm font-black text-red-700 hover:bg-red-200 disabled:opacity-60"
                  >
                    Rejeitar
                  </button>
                  <button
                    onClick={() => confirm(req)}
                    disabled={busy === req.id}
                    className="rounded-xl bg-violet-700 px-4 py-2 text-sm font-black text-white hover:bg-violet-800 disabled:opacity-60"
                  >
                    {busy === req.id ? "Confirmando..." : "Confirmar PIX recebido"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {done.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs font-black uppercase tracking-widest text-slate-400">
                  <th className="pb-3 pr-4">Motorista</th>
                  <th className="pb-3 pr-4 text-right">Valor</th>
                  <th className="pb-3 pr-4">Status</th>
                  <th className="pb-3 pr-4">Notas / Motivo</th>
                  <th className="pb-3 pr-4">Data</th>
                  <th className="pb-3">Revisado por</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {done.map((req) => (
                  <tr key={req.id} className="hover:bg-slate-50">
                    <td className="py-3 pr-4">
                      <p className="font-bold text-slate-800">{req.driver.name ?? "—"}</p>
                      <p className="text-xs text-slate-400">{req.driver.phone}</p>
                    </td>
                    <td className="py-3 pr-4 text-right font-black text-slate-700">{money(req.amountCents)}</td>
                    <td className="py-3 pr-4">
                      <Badge
                        value={req.status === "CONFIRMED" ? "Confirmado" : "Rejeitado"}
                        tone={req.status === "CONFIRMED" ? "green" : "red"}
                      />
                    </td>
                    <td className="py-3 pr-4 max-w-[180px] truncate text-slate-500">
                      {req.status === "REJECTED" ? (req.rejectionReason ?? "—") : (req.notes ?? "—")}
                    </td>
                    <td className="py-3 pr-4 text-slate-500 whitespace-nowrap">{dateTime(req.reviewedAt ?? req.requestedAt)}</td>
                    <td className="py-3 text-slate-500">{req.reviewer?.name ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </SectionCard>

      {rejectTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-sm rounded-3xl bg-white p-6 shadow-2xl">
            <h3 className="text-lg font-black text-slate-900">Rejeitar solicitação</h3>
            <p className="mt-1 text-sm font-semibold text-slate-500">
              {rejectTarget.driver.name} — {money(rejectTarget.amountCents)}
            </p>
            <label className="mt-4 block text-sm font-black text-slate-700">Motivo da rejeição</label>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Ex: Valor informado não confere com o recebido..."
              className="mt-2 w-full min-h-20 rounded-2xl border border-slate-200 p-3 text-sm font-semibold text-slate-700 outline-none focus:border-red-400"
            />
            {err && <p className="mt-2 text-sm font-bold text-red-600">{err}</p>}
            <div className="mt-4 grid grid-cols-2 gap-3">
              <button
                onClick={() => setRejectTarget(null)}
                className="rounded-2xl bg-slate-100 py-3 text-sm font-black text-slate-700 hover:bg-slate-200"
              >
                Cancelar
              </button>
              <button
                onClick={reject}
                disabled={busy !== null}
                className="rounded-2xl bg-red-600 py-3 text-sm font-black text-white hover:bg-red-700 disabled:opacity-60"
              >
                {busy ? "Rejeitando..." : "Rejeitar"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function SystemConfigPanel({
  config,
  authFetch,
  onChanged,
}: {
  config: SystemConfig;
  authFetch: AuthFetch;
  onChanged: () => void;
}) {
  const [limitInput, setLimitInput] = useState("");
  const [pixInput, setPixInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState("");

  useEffect(() => {
    setLimitInput(config.DRIVER_DEBT_LIMIT_CENTS ? (parseInt(config.DRIVER_DEBT_LIMIT_CENTS) / 100).toFixed(2).replace(".", ",") : "50,00");
    setPixInput(config.PLATFORM_PIX_KEY ?? "");
  }, [config]);

  const save = async () => {
    setSaving(true);
    setMsg("");
    try {
      const limitCents = Math.round(parseFloat(limitInput.replace(",", ".")) * 100);
      await Promise.all([
        authFetch("/admin/config", { method: "PATCH", body: JSON.stringify({ key: "DRIVER_DEBT_LIMIT_CENTS", value: String(limitCents) }) }),
        authFetch("/admin/config", { method: "PATCH", body: JSON.stringify({ key: "PLATFORM_PIX_KEY", value: pixInput.trim() }) }),
      ]);
      setMsg("Configurações salvas com sucesso.");
      onChanged();
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "Erro ao salvar");
    } finally {
      setSaving(false);
    }
  };

  return (
    <SectionCard title="Configurações da plataforma" subtitle="Regras de cobrança e dados de recebimento">
      <div className="grid gap-5 sm:grid-cols-2">
        <div>
          <label className="block text-sm font-black text-slate-700">Limite de saldo para bloqueio (R$)</label>
          <p className="mt-0.5 text-xs font-semibold text-slate-400">
            Motorista com saldo acima deste valor não consegue ficar online.
          </p>
          <input
            type="number"
            min="0"
            step="1"
            value={limitInput}
            onChange={(e) => setLimitInput(e.target.value)}
            className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 font-bold text-slate-800 outline-none focus:border-violet-500"
          />
        </div>
        <div>
          <label className="block text-sm font-black text-slate-700">Chave PIX da plataforma</label>
          <p className="mt-0.5 text-xs font-semibold text-slate-400">
            Exibida ao motorista quando ele solicitar quitação do saldo.
          </p>
          <input
            type="text"
            value={pixInput}
            onChange={(e) => setPixInput(e.target.value)}
            placeholder="email@, CPF, telefone ou chave aleatória"
            className="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 font-bold text-slate-800 outline-none focus:border-violet-500"
          />
        </div>
      </div>
      {msg && (
        <p className={classNames("mt-4 rounded-2xl p-3 text-sm font-bold", msg.includes("sucesso") ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700")}>
          {msg}
        </p>
      )}
      <div className="mt-5 flex justify-end">
        <button
          onClick={save}
          disabled={saving}
          className="rounded-2xl bg-violet-700 px-6 py-3 text-sm font-black text-white hover:bg-violet-800 disabled:opacity-60"
        >
          {saving ? "Salvando..." : "Salvar configurações"}
        </button>
      </div>
    </SectionCard>
  );
}
