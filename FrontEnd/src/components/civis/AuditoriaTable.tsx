import { useState, useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { Obra } from "@/lib/civis";

const riscoBadge = (r: string | null | undefined) => {
  const k = (r ?? "").toString().trim().toLowerCase();
  if (k === "alto") return "bg-red-500/20 text-red-300 border-red-500/40";
  if (k === "médio" || k === "medio")
    return "bg-amber-500/20 text-amber-300 border-amber-500/40";
  if (k === "baixo") return "bg-emerald-500/20 text-emerald-300 border-emerald-500/40";
  return "bg-slate-500/20 text-slate-300 border-slate-500/40";
};

export function AuditoriaTable({ obras }: { obras: Obra[] }) {
  const [open, setOpen] = useState<Obra | null>(null);
  
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 15; 

  useEffect(() => {
    setCurrentPage(1);
  }, [obras]);

  const totalPages = Math.ceil(obras.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  
  const paginatedObras = obras.slice(startIndex, endIndex);

  return (
    <div className="rounded-xl border border-white/10 bg-white/[0.02]">
      <div className="max-h-[480px] overflow-auto">
        <Table>
          <TableHeader>
            <TableRow className="bg-white/5">
              <TableHead>Bairro</TableHead>
              <TableHead>Zona</TableHead>
              <TableHead>Empreiteira</TableHead>
              <TableHead>Situação</TableHead>
              <TableHead>Risco</TableHead>
              <TableHead className="text-right">Área (m²)</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginatedObras.map((o, i) => (
              <TableRow
                key={(o.id as any) ?? i}
                onClick={() => setOpen(o)}
                className="cursor-pointer hover:bg-white/5"
              >
                <TableCell className="font-medium">{o.bairro ?? "—"}</TableCell>
                <TableCell className="text-muted-foreground">{o.zona_atuacao ?? "—"}</TableCell>
                <TableCell className="text-muted-foreground">{o.empreiteira ?? "—"}</TableCell>
                <TableCell>{o.situacao ?? "—"}</TableCell>
                <TableCell>
                  <Badge variant="outline" className={riscoBadge(o.risco_preditivo)}>
                    {o.risco_preditivo ?? "n/d"}
                  </Badge>
                </TableCell>
                <TableCell className="text-right tabular-nums">
                  {o.area
                    ? Number(o.area).toLocaleString("pt-BR", {
                        maximumFractionDigits: 0,
                      })
                    : "—"}
                </TableCell>
              </TableRow>
            ))}
            {obras.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                  Nenhuma obra encontrada para os filtros atuais.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {obras.length > 0 && (
        <div className="flex items-center justify-between border-t border-white/10 px-4 py-3">
          <div className="text-xs text-muted-foreground">
            Mostrando <span className="font-medium text-slate-300">{startIndex + 1}</span> a{" "}
            <span className="font-medium text-slate-300">
              {Math.min(endIndex, obras.length)}
            </span>{" "}
            de <span className="font-medium text-slate-300">{obras.length}</span> obras
          </div>
          
          <div className="flex gap-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-300 transition-colors hover:bg-white/10 disabled:pointer-events-none disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="rounded-md border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-300 transition-colors hover:bg-white/10 disabled:pointer-events-none disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      <Sheet open={!!open} onOpenChange={(v) => !v && setOpen(null)}>
        <SheetContent className="w-full overflow-y-auto sm:max-w-xl">
          <SheetHeader>
            <SheetTitle>Detalhe da obra</SheetTitle>
            <SheetDescription>
              Dados crus auditáveis · {open?.bairro ?? "—"} ({open?.zona_atuacao ?? "—"})
            </SheetDescription>
          </SheetHeader>
          {open && (
            <dl className="mt-6 grid grid-cols-1 gap-2 text-sm">
              {Object.entries(open).map(([k, v]) => (
                <div
                  key={k}
                  className="flex items-start justify-between gap-3 border-b border-white/5 py-2"
                >
                  <dt className="text-xs uppercase tracking-wider text-muted-foreground">
                    {k}
                  </dt>
                  <dd className="max-w-[60%] break-words text-right text-foreground">
                    {v === null || v === undefined || v === ""
                      ? "—"
                      : typeof v === "object"
                        ? JSON.stringify(v)
                        : String(v)}
                  </dd>
                </div>
              ))}
            </dl>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}