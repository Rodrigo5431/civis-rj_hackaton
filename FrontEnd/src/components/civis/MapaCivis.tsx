import { useEffect, useRef } from "react";
import { CircleMarker, MapContainer, Popup, TileLayer, useMap } from "react-leaflet";
import type { LatLngBoundsExpression, Map as LeafletMap } from "leaflet";
import { RIO_DAS_OSTRAS, type Obra } from "@/lib/civis";

function FitBounds({ obras }: { obras: Obra[] }) {
  const map = useMap();
  const fittedRef = useRef(false);

  useEffect(() => {
    const pts = obras
      .map((o) => [Number(o.latitude), Number(o.longitude)] as [number, number])
      .filter(([lat, lng]) => !isNaN(lat) && !isNaN(lng));

    if (pts.length === 0) {
      map.setView(RIO_DAS_OSTRAS, 13);
      return;
    }
    if (pts.length === 1) {
      map.setView(pts[0], 15);
      return;
    }
    const bounds: LatLngBoundsExpression = pts as LatLngBoundsExpression;
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
    fittedRef.current = true;
  }, [obras, map]);

  return null;
}

export function MapaCivis({ obras }: { obras: Obra[] }) {
  const mapRef = useRef<LeafletMap | null>(null);

  const BASE_LAT = -22.5269;
  const BASE_LNG = -41.945;

  // 1. FILTRO DE LIMPEZA VISUAL
  // Barramos qualquer obra que tenha "NÃO ESPECIFICADO" no nome do bairro
  const obrasFiltradas = obras.filter((o) => {
    const bairro = (o.bairro ?? "").toString().trim().toUpperCase();
    return !bairro.includes("NÃO ESPECIFICADO");
  });

  // 2. MOCK E JITTER (Aplicado apenas nas obras reais que sobraram)
  const obrasProcessadas = obrasFiltradas.map((o) => {
    let lat = Number(o.latitude);
    let lng = Number(o.longitude);

    if (!o.latitude || !o.longitude || isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0)) {
      lat = BASE_LAT + (Math.random() - 0.5) * 0.08;
      lng = BASE_LNG + (Math.random() - 0.5) * 0.08;
    } else {
      lat = lat + (Math.random() - 0.5) * 0.005;
      lng = lng + (Math.random() - 0.5) * 0.005;
    }

    return { ...o, latitude: lat as any, longitude: lng as any };
  });

  return (
    <div className="relative z-0 h-[460px] w-full overflow-hidden rounded-xl border border-white/10">
      <MapContainer
        center={RIO_DAS_OSTRAS}
        zoom={13}
        style={{ height: "100%", width: "100%", background: "#0b1220" }}
        ref={(m) => {
          if (m) mapRef.current = m;
        }}
        scrollWheelZoom
      >
        <TileLayer
          attribution='&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>'
          url="https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png"
        />
        <FitBounds obras={obrasProcessadas} />

        {obrasProcessadas.map((o, i) => {
          const isAlto = (o.risco_preditivo ?? "").toString().trim().toLowerCase() === "alto";
          return (
            <CircleMarker
              key={(o.id as any) ?? i}
              center={[Number(o.latitude), Number(o.longitude)]}
              radius={15}
              stroke={false}
              fillOpacity={isAlto ? 0.45 : 0.25}
              pathOptions={{
                fillColor: isAlto ? "#ef4444" : "#22d3ee",
              }}
            >
              <Popup>
                <div className="text-xs">
                  <div className="font-semibold">
                    {o.bairro ?? "Bairro n/d"} — {o.zona_atuacao ?? ""}
                  </div>
                  <div>Risco: {o.risco_preditivo ?? "n/d"}</div>
                  <div>Situação: {o.situacao ?? "n/d"}</div>
                  {o.empreiteira && <div>Empreiteira: {o.empreiteira}</div>}
                </div>
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>
    </div>
  );
}
