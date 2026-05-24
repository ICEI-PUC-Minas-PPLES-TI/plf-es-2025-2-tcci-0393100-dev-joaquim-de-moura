"use client";
import { MapContainer, TileLayer, Marker, Circle, Popup, useMapEvents } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

interface OperationRegion {
  id: string;
  name: string;
  city: string | null;
  active: boolean;
  centerLat: number | null;
  centerLng: number | null;
  radiusMeters: number | null;
}

interface RegionMapProps {
  regions: OperationRegion[];
  centerLat: number;
  centerLng: number;
  radiusMeters: number;
  onMapClick: (lat: number, lng: number) => void;
  onRegionClick: (region: OperationRegion) => void;
}

const BH_CENTER: [number, number] = [-19.9191, -43.9386];

function cursorIcon() {
  return L.divIcon({
    html: `<div style="background:#0f766e;width:22px;height:22px;border-radius:50%;border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,0.4)"></div>`,
    className: "",
    iconAnchor: [11, 11],
  });
}

function ClickHandler({ onMapClick }: { onMapClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onMapClick(
        Number(e.latlng.lat.toFixed(6)),
        Number(e.latlng.lng.toFixed(6))
      );
    },
  });
  return null;
}

export default function RegionMap({
  regions,
  centerLat,
  centerLng,
  radiusMeters,
  onMapClick,
  onRegionClick,
}: RegionMapProps) {
  return (
    <MapContainer
      center={BH_CENTER}
      zoom={12}
      style={{ width: "100%", height: "100%" }}
      zoomControl
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <ClickHandler onMapClick={onMapClick} />

      {/* Marcador da posicao sendo editada */}
      <Marker position={[centerLat, centerLng]} icon={cursorIcon()} />
      <Circle
        center={[centerLat, centerLng]}
        radius={radiusMeters}
        pathOptions={{ color: "#0f766e", fillColor: "#0f766e", fillOpacity: 0.12, weight: 2 }}
      />

      {/* Regioes cadastradas */}
      {regions.map((region) =>
        region.centerLat != null && region.centerLng != null ? (
          <Circle
            key={region.id}
            center={[region.centerLat, region.centerLng]}
            radius={region.radiusMeters ?? 5000}
            pathOptions={{
              color: region.active ? "#0f766e" : "#94a3b8",
              fillColor: region.active ? "#0f766e" : "#94a3b8",
              fillOpacity: 0.1,
              weight: 2,
              dashArray: region.active ? undefined : "6 4",
            }}
            eventHandlers={{ click: () => onRegionClick(region) }}
          >
            <Popup>
              <strong>{region.name}</strong>
              <br />
              {region.city ?? "Cidade não informada"}
              <br />
              <span style={{ color: region.active ? "#0f766e" : "#64748b", fontWeight: 700 }}>
                {region.active ? "Ativa" : "Pausada"}
              </span>
              <br />
              <span style={{ fontSize: 12, color: "#64748b" }}>
                Clique para editar
              </span>
            </Popup>
          </Circle>
        ) : null
      )}
    </MapContainer>
  );
}
