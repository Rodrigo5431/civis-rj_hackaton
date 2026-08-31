import { useState, type FormEvent, useEffect, useRef } from "react";
import { Card } from "@/components/ui/card";
import {
  Bot,
  CheckCircle,
  CheckCircle2,
  FileSignature,
  Loader2,
  Sparkles,
  Wand2,
  Globe,
  Server,
  Check,
  RefreshCw,
} from "lucide-react";

declare global {
  interface Window {
    NutrientViewer?: any;
  }
}

type AuditStatus =
  | "PENDING_EXTRACTION"
  | "PROCESSING"
  | "EXTRACTED"
  | "FAILED"
  | "APPROVED"
  | "REJECTED";

interface AuditUploadResponse {
  id: string;
  idObra: string;
  status: AuditStatus;
  fileName: string;
  dwsDocumentId: string;
  dwsViewerUrl: string;
  uploadedAt: string;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const OPENROUTER_API_KEY = import.meta.env.VITE_OPENROUTER_API_KEY;
const NUTRIENT_PUBLISHABLE_KEY = import.meta.env.VITE_NUTRIENT_PUBLISHABLE_KEY;

function NutrientViewerEmbed({ file }: { file: File }) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [isLoadingSdk, setIsLoadingSdk] = useState(true);

  useEffect(() => {
    if (!window.NutrientViewer) {
      const script = document.createElement("script");
      script.src = "https://cdn.cloud.nutrient.io/pspdfkit-web@1.17.0/nutrient-viewer.js";
      script.async = true;
      script.onload = () => setIsLoadingSdk(false);
      document.body.appendChild(script);
    } else {
      setIsLoadingSdk(false);
    }
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    
    const loadViewer = async () => {
      if (!isLoadingSdk && container && window.NutrientViewer && file) {
        try {
          const arrayBuffer = await file.arrayBuffer();
          
          window.NutrientViewer.unload(container);
          await window.NutrientViewer.load({
            container,
            document: arrayBuffer,
            session: NUTRIENT_PUBLISHABLE_KEY, 
          });
          
          console.log("✅ Visualizador Nutrient DWS carregado com sucesso!");
        } catch (error) {
          console.error("⚠️ Erro ao carregar o Nutrient DWS:", error);
        }
      }
    };

    loadViewer();

    return () => {
      if (container && window.NutrientViewer) {
        window.NutrientViewer.unload(container);
      }
    };
  }, [isLoadingSdk, file]);

  return (
    <div className="w-full h-[450px] bg-slate-900 rounded-lg overflow-hidden border border-white/10 shadow-inner relative flex items-center justify-center">
      {isLoadingSdk && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-xs text-slate-400 bg-slate-950/80 z-10">
          <Loader2 className="animate-spin text-cyan-500" size={24} /> 
          Carregando motor Nutrient...
        </div>
      )}
      <div ref={containerRef} className="w-full h-full" />
    </div>
  );
}

interface OfficialDocumentSectionProps {
  auditId: string;
  idObra: string;
  aiVerdict: string;
}

function OfficialDocumentSection({
  auditId,
  idObra,
  aiVerdict,
}: OfficialDocumentSectionProps) {
  const [doctavianUrl, setDoctavianUrl] = useState<string | null>(null);
  const [isGeneratingDoc, setIsGeneratingDoc] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleGenerateOfficialDocument = async () => {
    setIsGeneratingDoc(true);
    setErrorMessage(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/${auditId}/generate-official-document`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            aiVerdict: aiVerdict,
            idObra: idObra,
          }),
        },
      );

      if (!response.ok) {
        const errData = await response.json().catch(() => null);
        throw new Error(errData?.error || "Falha ao gerar documento oficial via Doctavian");
      }

      const data = await response.json();
      setDoctavianUrl(data.document_url);
    } catch (error: any) {
      console.error(error);
      setErrorMessage(error.message || "Erro ao conectar com o servidor.");
    } finally {
      setIsGeneratingDoc(false);
    }
  };

  return (
    <div className="mt-6 border-t border-slate-700 pt-5">
      <h3 className="text-lg font-semibold text-white mb-2">Operações de Fechamento (Doctavian)</h3>
      <p className="text-sm text-slate-400 mb-4">
        Transforme o laudo técnico em um Termo Oficial de Notificação pronto para assinatura digital.
      </p>

      {!doctavianUrl ? (
        <button
          onClick={handleGenerateOfficialDocument}
          disabled={isGeneratingDoc || !aiVerdict}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-medium py-2.5 px-4 rounded-lg transition-colors shadow-lg w-full justify-center lg:w-max"
        >
          {isGeneratingDoc ? (
            <>
              <Loader2 className="animate-spin" size={20} />
              Gerando Termo Oficial (Doctavian)...
            </>
          ) : (
            <>
              <FileSignature size={20} />
              Gerar e Assinar Termo Oficial
            </>
          )}
        </button>
      ) : (
        <div className="p-4 bg-emerald-950/40 border border-emerald-500/30 rounded-lg text-emerald-300 flex items-start gap-3">
          <CheckCircle2 className="text-emerald-400 mt-0.5 shrink-0" size={20} />
          <div>
            <p className="font-semibold">Termo Oficial Gerado com Sucesso!</p>
            <p className="text-xs text-emerald-400/80 mb-2">
              O documento foi estruturado e enviado para o fluxo de assinatura.
            </p>
            <a
              href={doctavianUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-block text-xs bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-1.5 px-3 rounded transition-colors"
            >
              Abrir Documento Oficial (PDF) ↗
            </a>
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="mt-3 p-3 bg-red-950/40 border border-red-500/30 rounded-lg text-red-300 text-xs">
          ⚠️ {errorMessage}
        </div>
      )}
    </div>
  );
}

function TransparencyPortalSection({ cityName }: { cityName: string }) {
  const [domains, setDomains] = useState<any[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [registeredDomain, setRegisteredDomain] = useState<string | null>(null);
  
  const [registeringDomain, setRegisteringDomain] = useState<string | null>(null);

  const handleSearchDomains = async () => {
    setIsSearching(true);
    setHasSearched(false);
    try {
      const response = await fetch(
        `${API_BASE_URL}/transparency/domain-search?cityName=${encodeURIComponent(cityName)}`,
      );
      const data = await response.json();
      
      if (data.results && data.results.length > 0) {
        setDomains(data.results.slice(0, 3));
      } else {
        setDomains([]);
      }
    } catch (error) {
      console.error("Erro ao buscar domínios na Name.com", error);
      setDomains([]);
    } finally {
      setIsSearching(false);
      setHasSearched(true);
    }
  };

  const handleRegister = async (domainName: string) => {
    setRegisteringDomain(domainName);
    try {
      const response = await fetch(`${API_BASE_URL}/transparency/domain-register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ domainName })
      });
      
      if (!response.ok) throw new Error("Falha no registro na Name.com");
      
      setRegisteredDomain(domainName);
    } catch (error) {
      console.error("Erro ao registrar o domínio via backend", error);
      alert("Erro ao registrar o domínio. Verifique sua conexão ou créditos da Name.com.");
    } finally {
      setRegisteringDomain(null);
    }
  };

  return (
    <div className="mt-6 border-t border-slate-700 pt-5">
      <h3 className="text-lg font-semibold text-white mb-2 flex items-center gap-2">
        <Globe size={18} className="text-blue-400" />
        Portal de Transparência (Name.com)
      </h3>
      <p className="text-sm text-slate-400 mb-4">
        Disponibilize os dados desta auditoria para a população. Busque e provisione um domínio dedicado instantaneamente.
      </p>

      {!registeredDomain ? (
        <>
          {domains.length === 0 ? (
            <div className="flex flex-col gap-3">
              <button
                onClick={handleSearchDomains}
                disabled={isSearching}
                className="bg-slate-800 hover:bg-slate-700 border border-slate-600 text-slate-200 py-2.5 px-4 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 shadow-lg w-full justify-center lg:w-max"
              >
                {isSearching ? <Loader2 className="animate-spin" size={16} /> : <Globe size={16} />}
                {isSearching ? "Consultando Name.com API..." : "Consultar Domínios Disponíveis"}
              </button>
              {hasSearched && !isSearching && (
                <p className="text-xs text-red-400">
                  Nenhum domínio disponível foi retornado pela API para esta consulta.
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-3 animate-in fade-in duration-300">
              <p className="text-xs text-blue-300 uppercase tracking-wider font-semibold">
                Opções Reais Disponíveis na Name.com:
              </p>
              {domains.map((d, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between bg-slate-950/50 border border-slate-700 p-3 rounded-lg hover:border-blue-500/50 transition-colors"
                >
                  <div>
                    <p className="font-mono text-slate-200 text-sm">{d.domainName}</p>
                    <p className="text-xs text-slate-400 mt-0.5">Preço: ${d.purchasePrice}/ano</p>
                  </div>
                  <button
                    onClick={() => handleRegister(d.domainName)}
                    disabled={registeringDomain !== null}
                    className="bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-bold py-2 px-3 rounded shadow-md flex items-center gap-1.5 transition-colors"
                  >
                    {registeringDomain === d.domainName ? (
                      <Loader2 className="animate-spin" size={14} />
                    ) : (
                      <Server size={14} />
                    )}
                    {registeringDomain === d.domainName ? "Registrando..." : "Registrar & Apontar"}
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="p-4 bg-blue-950/40 border border-blue-500/30 rounded-lg flex items-start gap-3 animate-in fade-in zoom-in duration-300">
          <Check className="text-blue-400 mt-0.5 shrink-0" size={20} />
          <div>
            <p className="font-semibold text-blue-300">Domínio Selecionado para Provisionamento!</p>
            <p className="text-xs text-blue-200/80 mt-1">
              O domínio{" "}
              <span className="font-mono text-white bg-blue-900/80 px-1.5 py-0.5 rounded mx-1">
                {registeredDomain}
              </span>{" "}
              foi registrado via Name.com API. O DNS está sendo propagado para o painel público.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

export function ContractAuditor() {
  const [idObra, setIdObra] = useState<string>("");
  const [file, setFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [audit, setAudit] = useState<AuditUploadResponse | null>(null);

  const [isApproved, setIsApproved] = useState<boolean>(false);
  const [isApproving, setIsApproving] = useState<boolean>(false);

  const [parecerIA, setParecerIA] = useState<string | null>(null);
  const [isGeneratingParecer, setIsGeneratingParecer] = useState<boolean>(false);

  const [formKey, setFormKey] = useState<number>(0);

  function handleReset() {
    setIdObra("");
    setFile(null);
    setAudit(null);
    setIsApproved(false);
    setParecerIA(null);
    setError(null);
    setFormKey((prev) => prev + 1);
  }

  async function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!idObra || !file) {
      setError("Informe o ID da obra e selecione um PDF.");
      return;
    }
    if (file.type !== "application/pdf") {
      setError("Formato inválido. Por favor, envie apenas arquivos no formato PDF.");
      return;
    }

    const MAX_FILE_SIZE = 10 * 1024 * 1024;
    if (file.size > MAX_FILE_SIZE) {
      setError("O arquivo é muito grande. O limite máximo é de 10MB.");
      return;
    }

    setIsUploading(true);
    setError(null);
    setParecerIA(null);

    const formData = new FormData();
    formData.append("id_obra", idObra.trim());
    formData.append("file", file);

    try {
      const response = await fetch(`${API_BASE_URL}/upload`, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        if (errorData && errorData.error) {
          throw new Error(errorData.error);
        }
        throw new Error(`Falha no upload (status ${response.status})`);
      }

      const data: AuditUploadResponse = await response.json();
      setAudit(data);
      setIsApproved(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro desconhecido no upload.");
    } finally {
      setIsUploading(false);
    }
  }

  async function handleApprove() {
    if (!audit || isApproving) return;

    setIsApproving(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/${audit.id}/approve`, {
        method: "PATCH",
      });
      if (!response.ok) {
        throw new Error(`Falha ao aprovar (status ${response.status})`);
      }
      setIsApproved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao aprovar contrato.");
    } finally {
      setIsApproving(false);
    }
  }

  async function handleGerarParecerIA() {
    if (!audit) return;

    setIsGeneratingParecer(true);
    setError(null);

    try {
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${OPENROUTER_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "meta-llama/llama-3.1-8b-instruct",
          max_tokens: 250, 
          messages: [
            {
              role: "system",
              content:
                "Você é o Copiloto Civis, um auditor de obras públicas. Responda estritamente com UM ÚNICO PARÁGRAFO de no máximo 500 caracteres. Seja direto, técnico e formal. Proibido usar introduções longas ou listas.",
            },
            {
              role: "user",
              content: `Gere o parecer final de auditoria para a obra ${audit.idObra}. 
              Dados: Extração de Contrato (Nutrient DWS) validada sem adulterações. Varredura web (SerpApi) sem riscos. 
              Conclua recomendando a liberação do orçamento para início das atividades.`,
            },
          ],
        }),
      });

      if (!response.ok) throw new Error("Falha ao gerar parecer com a IA.");

      const data = await response.json();
      let textoGerado = data?.choices?.[0]?.message?.content?.trim();
      
      if (!textoGerado) {
        throw new Error("A IA retornou uma resposta vazia.");
      }

      if (textoGerado.length > 670) {
        textoGerado = textoGerado.substring(0, 667) + "...";
      }

      setParecerIA(textoGerado);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao gerar parecer.");
    } finally {
      setIsGeneratingParecer(false);
    }
  }

  const isFormLocked = audit !== null;

  return (
    <Card className="border-white/10 bg-white/[0.03] p-8 text-slate-100 w-full min-h-[600px]">
      <div className="mb-8 border-b border-white/10 pb-4 flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold uppercase tracking-widest text-cyan-300">
            Auditoria de Contratos
          </h2>
          <p className="text-sm text-slate-400 mt-1">Powered by Nutrient DWS API</p>
        </div>
        {isApproved && (
          <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-3 py-1 rounded-full text-xs font-semibold flex items-center gap-1">
            <CheckCircle className="w-3 h-3" />
            Contrato Validado
          </span>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <div>
          <form onSubmit={handleUpload} className="flex flex-col gap-5">
            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-slate-300">
                ID da Obra / Processo Licitatório
              </span>
              <input
                type="text"
                value={idObra}
                onChange={(e) => setIdObra(e.target.value)}
                disabled={isFormLocked || isUploading}
                className="rounded-md border border-white/10 bg-slate-900/50 px-4 py-3 text-sm text-slate-100 placeholder:text-slate-500 focus:border-cyan-500 focus:outline-none focus:ring-1 focus:ring-cyan-500 transition-all shadow-inner disabled:opacity-50 disabled:cursor-not-allowed"
                placeholder="Ex: 900230334668"
              />
            </label>

            <label className="flex flex-col gap-2">
              <span className="text-sm font-medium text-slate-300">Documento (PDF)</span>
              <input
                key={formKey}
                type="file"
                accept="application/pdf"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                disabled={isFormLocked || isUploading}
                className="rounded-md border border-white/10 bg-slate-900/50 px-4 py-3 text-sm text-slate-100 file:mr-4 file:rounded file:border-0 file:bg-cyan-500/10 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-cyan-300 hover:file:bg-cyan-500/20 cursor-pointer transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              />
            </label>

            {!isFormLocked ? (
              <button
                type="submit"
                disabled={isUploading}
                className="mt-4 w-full md:w-max rounded-md bg-cyan-600 px-6 py-3 text-sm font-semibold text-white shadow-lg hover:bg-cyan-500 disabled:opacity-50 transition-colors flex items-center justify-center gap-2"
              >
                {isUploading && <Loader2 className="animate-spin" size={16} />}
                {isUploading ? "Processando Documento..." : "Enviar para Extração (Nutrient)"}
              </button>
            ) : (
              <button
                type="button"
                onClick={handleReset}
                className="mt-4 w-full md:w-max rounded-md bg-slate-700 px-6 py-3 text-sm font-semibold text-white shadow-lg hover:bg-slate-600 transition-colors flex items-center justify-center gap-2"
              >
                <RefreshCw size={16} />
                Iniciar Nova Auditoria
              </button>
            )}

            {error && (
              <div className="mt-4 p-3 bg-red-900/30 border border-red-800 rounded text-sm text-red-400">
                {error}
              </div>
            )}
          </form>
        </div>

        <div>
          {audit && file ? (
            <div className="rounded-xl border border-white/10 bg-slate-900/40 p-6 h-full flex flex-col shadow-inner animate-in fade-in duration-300">
              <div className="mb-6 flex flex-wrap gap-6 text-sm border-b border-white/5 pb-4">
                <p>
                  <strong className="text-cyan-400 uppercase text-xs tracking-wider">Status</strong>
                  <br /> <span className="font-medium">{audit.status}</span>
                </p>
                <p>
                  <strong className="text-cyan-400 uppercase text-xs tracking-wider">DWS ID</strong>
                  <br /> <span className="font-mono text-xs">{audit.dwsDocumentId}</span>
                </p>
              </div>

              <div className="mb-6">
                <p className="mb-2 font-semibold text-slate-300 text-sm uppercase tracking-wide flex items-center gap-2">
                  <Globe size={16} className="text-cyan-400" />
                  Visualizador Oficial (Nutrient DWS)
                </p>
                <NutrientViewerEmbed file={file} />
              </div>

              {!isApproved ? (
                <button
                  type="button"
                  onClick={handleApprove}
                  disabled={isApproving}
                  className="w-full rounded-md bg-emerald-600 px-5 py-3 text-sm font-bold text-white shadow-lg hover:bg-emerald-500 disabled:opacity-50 transition-all flex justify-center items-center gap-2"
                >
                  {isApproving ? (
                    <Loader2 className="w-5 h-5 animate-spin" />
                  ) : (
                    <CheckCircle className="w-5 h-5" />
                  )}
                  {isApproving ? "Aprovando..." : "Aprovar Extração de Dados"}
                </button>
              ) : (
                <div className="space-y-4 animate-in fade-in zoom-in duration-300">
                  {!parecerIA ? (
                    <button
                      type="button"
                      onClick={handleGerarParecerIA}
                      disabled={isGeneratingParecer}
                      className="w-full rounded-md bg-indigo-600 px-5 py-3 text-sm font-bold text-white shadow-lg hover:bg-indigo-500 disabled:opacity-50 transition-all flex justify-center items-center gap-2 border border-indigo-400/30"
                    >
                      {isGeneratingParecer ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                      ) : (
                        <Sparkles className="w-5 h-5 text-indigo-200" />
                      )}
                      {isGeneratingParecer ? "Analisando Dados..." : "Gerar Parecer Final com IA"}
                    </button>
                  ) : (
                    <>
                      <div className="bg-indigo-950/40 border border-indigo-500/30 rounded-lg p-5">
                        <div className="flex items-center gap-2 mb-3 text-indigo-300">
                          <Bot className="w-5 h-5" />
                          <h3 className="font-semibold text-sm uppercase tracking-wide">
                            Parecer do Copiloto
                          </h3>
                        </div>
                        <p className="text-sm text-indigo-100 leading-relaxed">{parecerIA}</p>
                      </div>

                      <OfficialDocumentSection
                        auditId={audit.id}
                        idObra={audit.idObra}
                        aiVerdict={parecerIA}
                      />

                      <TransparencyPortalSection cityName="petropolis" />
                    </>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="rounded-xl border border-white/10 bg-slate-900/20 p-8 h-full flex flex-col items-center justify-center text-slate-500 border-dashed text-center">
              <Wand2 className="w-12 h-12 mb-4 text-slate-700" />
              <p className="font-medium text-slate-400">Nenhum contrato em análise</p>
              <p className="text-sm mt-2">
                Faça o upload do documento ao lado para iniciar a extração de dados com a Nutrient
                DWS.
              </p>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
}