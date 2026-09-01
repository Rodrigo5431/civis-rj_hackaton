import { useRef, useState } from "react";
import { MessageSquare, Send, Sparkles, X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import type { Obra } from "@/lib/civis";


type Msg = { role: "user" | "assistant" | "system"; content: string };

const SYSTEM_PROMPT = (n: number) =>
  `Você é o Copiloto Civis, assistente analítico do Centro de Comando Preditivo de Obras Públicas do Rio de Janeiro.
Você recebe um resumo agregado de ${n} obras. Responda em português brasileiro, de forma direta, com bullets curtos quando útil.
Foque em risco fiscal, atrasos, padrões de empreiteiras e priorização operacional.`;

function buildContext(obras: Obra[]) {
  const total = obras.length;
  const ativas = obras.filter(
    (o) => ["em andamento", "atrasada"].includes((o.situacao ?? "").trim().toLowerCase()),
  ).length;
  const alto = obras.filter((o) => (o.risco_preditivo ?? "").trim().toLowerCase() === "alto").length;
  const porZona: Record<string, number> = {};
  for (const o of obras) {
    const z = (o.zona_atuacao ?? "n/d").toString().trim() || "n/d";
    porZona[z] = (porZona[z] ?? 0) + 1;
  }
  return `Resumo: total=${total}, ativas=${ativas}, risco_alto=${alto}, por_zona=${JSON.stringify(porZona)}`;
}

export function CopilotoCivis({ obras }: { obras: Obra[] }) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Msg[]>([
    {
      role: "assistant",
      content:
        "Olá! Sou o Copiloto Civis. Pergunte sobre risco, ranking de bairros, padrões de empreiteiras ou priorização de auditoria.",
    },
  ]);
  const scrollRef = useRef<HTMLDivElement>(null);

  const API_HOST = import.meta.env.VITE_API_BASE_URL.split("/api/")[0];

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    
    const next = [...messages, { role: "user" as const, content: text }];
    setMessages(next);
    setInput("");
    setLoading(true);
    
    try {
      const res = await fetch(`${API_HOST}/api/chat`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          messages: [
            { role: "system", content: SYSTEM_PROMPT(obras.length) },
            { role: "system", content: buildContext(obras) },
            ...next.map((m) => ({ role: m.role, content: m.content })),
          ],
        }),
      });

      if (!res.ok) {
        throw new Error("Falha ao comunicar com o servidor Civis.");
      }

      const data = await res.json();
      
      const reply = data.reply || data.resposta || data?.choices?.[0]?.message?.content || "Resposta não identificada no servidor.";
      
      setMessages((prev) => [...prev, { role: "assistant", content: reply }]);
    } catch (e: any) {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "Erro de comunicação: " + (e?.message ?? "desconhecido") },
      ]);
    } finally {
      setLoading(false);
      requestAnimationFrame(() => {
        const el = scrollRef.current;
        if (el) el.scrollTop = el.scrollHeight;
      });
    }
  };

  return (
    <>
      <button
        onClick={() => setOpen((v) => !v)}
        aria-label="Abrir Copiloto Civis"
        className="fixed bottom-6 right-6 z-50 inline-flex items-center gap-2 rounded-full border border-cyan-400/40 bg-gradient-to-br from-cyan-500 to-sky-700 px-5 py-3 text-sm font-semibold text-white shadow-[0_0_30px_rgba(34,211,238,0.45)] transition hover:scale-105"
      >
        <Sparkles className="size-4" />
        Copiloto Civis
      </button>

      {open && (
        <Card className="fixed bottom-24 right-6 z-50 flex h-[520px] w-[min(92vw,400px)] flex-col overflow-hidden border-white/10 bg-slate-950/95 text-slate-100 backdrop-blur-xl">
          <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
            <div className="flex items-center gap-2">
              <MessageSquare className="size-4 text-cyan-300" />
              <span className="text-sm font-semibold">Copiloto Civis · IA</span>
            </div>
            <button
              onClick={() => setOpen(false)}
              aria-label="Fechar"
              className="rounded-md p-1 text-muted-foreground hover:bg-white/10 transition-colors"
            >
              <X className="size-4" />
            </button>
          </div>

          <ScrollArea className="flex-1 px-4 py-3" ref={scrollRef as any}>
            <div className="space-y-3">
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${
                    m.role === "user"
                      ? "ml-auto bg-cyan-500/20 text-cyan-50"
                      : "bg-white/5 text-slate-100 border border-white/5"
                  }`}
                >
                  <p className="whitespace-pre-wrap leading-relaxed">{m.content}</p>
                </div>
              ))}
              {loading && (
                <div className="bg-white/5 max-w-[85%] rounded-lg px-3 py-2 text-sm text-cyan-400 flex items-center gap-2 border border-white/5">
                  <Loader2 className="animate-spin" size={14} />
                  Analisando dados táticos...
                </div>
              )}
            </div>
          </ScrollArea>

          <div className="border-t border-white/10 p-3 bg-slate-950">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                send();
              }}
              className="flex items-center gap-2"
            >
              <Input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ex: quais bairros priorizar?"
                className="border-white/10 bg-white/5 text-sm focus-visible:ring-cyan-500/50"
                disabled={loading}
              />
              <Button
                type="submit"
                size="icon"
                disabled={loading || !input.trim()}
                className="bg-cyan-600 hover:bg-cyan-500 text-white transition-colors"
              >
                <Send className="size-4" />
              </Button>
            </form>
          </div>
        </Card>
      )}
    </>
  );
}