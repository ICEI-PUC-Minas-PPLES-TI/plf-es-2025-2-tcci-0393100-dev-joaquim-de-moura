"use client";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

interface LiveDriver {
  id: string;
  name: string | null;
  phone: string;
  available: boolean;
  currentLat: number | null;
  currentLng: number | null;
  vehicleModel: string | null;
  vehiclePlate: string | null;
}

interface LiveRide {
  id: string;
  status: string;
  estimatedFareCents: number | null;
  originLat: number;
  originLng: number;
  passenger: { name: string | null; phone: string } | null;
}

interface LiveMapProps {
  drivers: LiveDriver[];
  rides: LiveRide[];
}

const BH_CENTER: [number, number] = [-19.9191, -43.9386];

function driverIcon(available: boolean) {
  return L.divIcon({
    html: `<div style="background:${available ? "#0f766e" : "#b45309"};color:white;padding:4px 10px;border-radius:20px;border:3px solid white;font-size:11px;font-weight:900;white-space:nowrap;box-shadow:0 2px 8px rgba(0,0,0,0.35)">CAR</div>`,
    className: "",
    iconAnchor: [24, 16],
  });
}

function rideIcon() {
  return L.divIcon({
    html: `<div style="background:#b91c1c;color:white;width:32px;height:32px;border-radius:50%;border:3px solid white;font-size:12px;font-weight:900;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 8px rgba(0,0,0,0.35)">R</div>`,
    className: "",
    iconAnchor: [16, 16],
  });
}

export default function LiveMap({ drivers, rides }: LiveMapProps) {
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
      {drivers.map((driver) =>
        driver.currentLat != null && driver.currentLng != null ? (
          <Marker
            key={driver.id}
            position={[driver.currentLat, driver.currentLng]}
            icon={driverIcon(driver.available)}
          >
            <Popup>
              <strong>{driver.name ?? "Motorista"}</strong>
              <br />
              {driver.phone}
              <br />
              {driver.vehicleModel ?? "Veículo não informado"}
              {driver.vehiclePlate ? ` — ${driver.vehiclePlate}` : ""}
              <br />
              <span style={{ color: driver.available ? "#0f766e" : "#b45309", fontWeight: 700 }}>
                {driver.available ? "Disponível" : "Ocupado"}
              </span>
            </Popup>
          </Marker>
        ) : null
      )}
      {rides.map((ride) => (
        <Marker
          key={ride.id}
          position={[ride.originLat, ride.originLng]}
          icon={rideIcon()}
        >
          <Popup>
            <strong>{ride.passenger?.name ?? "Passageiro"}</strong>
            <br />
            {ride.passenger?.phone}
            <br />
            Status: {ride.status}
            {ride.estimatedFareCents != null
              ? `\nR$ ${(ride.estimatedFareCents / 100).toFixed(2).replace(".", ",")}`
              : ""}
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
