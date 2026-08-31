import { createClient } from "@supabase/supabase-js";

export const OBRAS_SUPABASE_URL = 
  import.meta.env.VITE_SUPABASE_URL;

export const OBRAS_SUPABASE_ANON = 
  import.meta.env.VITE_SUPABASE_ANON_KEY;

export const obrasSupabase = createClient(OBRAS_SUPABASE_URL, OBRAS_SUPABASE_ANON, {
  auth: { persistSession: false },
});

export type Obra = Record<string, any> & {
  id?: string | number;
  situacao?: string | null;
  zona_atuacao?: string | null;
  bairro?: string | null;
  risco_preditivo?: string | null;
  empreiteira?: string | null;
  area?: number | null;
  latitude?: number | null;
  longitude?: number | null;
};

const norm = (v: unknown) => typeof v === "string" ? v.trim().toLowerCase() : "";

export const isObraAtiva = (o: Obra) => {
  const s = norm(o.situacao);
  return s === "em andamento" || s === "atrasada";
};

export const isRiscoAlto = (o: Obra) => norm(o.risco_preditivo) === "alto";
export const isRiscoMedio = (o: Obra) => norm(o.risco_preditivo) === "médio" || norm(o.risco_preditivo) === "medio";
export const isRiscoBaixo = (o: Obra) => norm(o.risco_preditivo) === "baixo";

export const numero = (v: unknown): number => {
  if (v === null || v === undefined) return 0;
  const n = typeof v === "number" ? v : parseFloat(String(v).replace(",", "."));
  return Number.isFinite(n) ? n : 0;
};

export const formatBRL = (n: number) =>
  n.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  });

export const RIO_DAS_OSTRAS: [number, number] = [-22.5269, -41.945];

export const RISCO_COLORS = {
  Alto: "#ef4444",
  Médio: "#f59e0b",
  Baixo: "#10b981",
} as const;

export const RECHARTS_TOOLTIP_STYLE = {
  backgroundColor: "#0f172a",
  borderColor: "#334155",
  color: "#f8fafc",
  borderRadius: 8,
  fontSize: 12,
} as const;