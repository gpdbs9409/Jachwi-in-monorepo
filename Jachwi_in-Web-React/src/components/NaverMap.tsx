import { useCallback, useEffect, useRef, useState } from "react";
import { fetchBuildingsInArea } from "../api/map";
import { MapChatPanel } from "./MapChatPanel";
import type { Building, MapBounds, RecommendedBuilding } from "../api/types";

declare global {
  interface Window {
    naver: any;
  }
}

const CLIENT_ID = import.meta.env.VITE_NAVER_MAP_CLIENT_ID as string | undefined;
const SCRIPT_ID = "naver-maps-sdk";

// 기본 중심좌표: 한양대학교 서울캠퍼스 부근 (자취인 캡스톤 기준 학교)
const DEFAULT_CENTER = { lat: 37.5567, lng: 127.0432 };

function loadNaverMapsScript(clientId: string): Promise<void> {
  if (window.naver?.maps) return Promise.resolve();

  const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
  if (existing) {
    return new Promise((resolve, reject) => {
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("네이버 지도 스크립트 로드 실패")));
    });
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.id = SCRIPT_ID;
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${clientId}`;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("네이버 지도 스크립트 로드 실패"));
    document.head.appendChild(script);
  });
}

export function NaverMap() {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<any>(null);
  const markers = useRef<any[]>([]);
  const highlightMarkers = useRef<any[]>([]);
  const [buildings, setBuildings] = useState<Building[]>([]);
  const [bounds, setBounds] = useState<MapBounds | null>(null);
  const [highlighted, setHighlighted] = useState<RecommendedBuilding[]>([]);
  const [status, setStatus] = useState<"loading" | "ready" | "no-client-id" | "error">(
    "loading"
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadBuildingsInBounds = useCallback(async () => {
    const map = mapInstance.current;
    if (!map) return;
    const mapBounds = map.getBounds();
    const sw = mapBounds.getSW();
    const ne = mapBounds.getNE();
    const nextBounds: MapBounds = { minX: sw.x, maxX: ne.x, minY: sw.y, maxY: ne.y };
    setBounds(nextBounds);
    try {
      const data = await fetchBuildingsInArea(nextBounds);
      setBuildings(data);
    } catch (err) {
      console.error("건물 조회 실패", err);
    }
  }, []);

  useEffect(() => {
    if (!CLIENT_ID) {
      setStatus("no-client-id");
      return;
    }

    let cancelled = false;

    loadNaverMapsScript(CLIENT_ID)
      .then(() => {
        if (cancelled || !mapRef.current) return;
        const { naver } = window;
        const map = new naver.maps.Map(mapRef.current, {
          center: new naver.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng),
          zoom: 15,
        });
        mapInstance.current = map;
        naver.maps.Event.addListener(map, "idle", loadBuildingsInBounds);
        setStatus("ready");
        loadBuildingsInBounds();
      })
      .catch((err) => {
        console.error(err);
        setStatus("error");
        setErrorMessage(err.message);
      });

    return () => {
      cancelled = true;
    };
  }, [loadBuildingsInBounds]);

  // 건물 목록이 바뀌면 일반 마커를 다시 그린다.
  useEffect(() => {
    const { naver } = window;
    const map = mapInstance.current;
    if (!naver || !map) return;

    markers.current.forEach((m) => m.setMap(null));
    markers.current = buildings.map((b) => {
      const marker = new naver.maps.Marker({
        position: new naver.maps.LatLng(b.y, b.x),
        map,
        title: b.localBuildingName ?? b.officialBuildingName ?? undefined,
      });
      const infoWindow = new naver.maps.InfoWindow({
        content: `<div style="padding:8px 10px;font-size:13px;">
          <b>${b.localBuildingName ?? b.officialBuildingName ?? "건물"}</b><br/>
          카페 ${b.cafe ?? 0} · 편의점 ${b.convenienceStore ?? 0} · CCTV ${b.cctv ?? 0}
        </div>`,
      });
      naver.maps.Event.addListener(marker, "click", () => {
        infoWindow.open(map, marker);
      });
      return marker;
    });
  }, [buildings]);

  // AI가 추천한 건물은 별도의 강조 마커로 겹쳐 그리고, 지도를 그 범위로 맞춘다.
  useEffect(() => {
    const { naver } = window;
    const map = mapInstance.current;
    if (!naver || !map) return;

    highlightMarkers.current.forEach((m) => m.setMap(null));
    if (highlighted.length === 0) {
      highlightMarkers.current = [];
      return;
    }

    const fitBounds = new naver.maps.LatLngBounds();

    highlightMarkers.current = highlighted.map((b, idx) => {
      const position = new naver.maps.LatLng(b.y, b.x);
      fitBounds.extend(position);
      const marker = new naver.maps.Marker({
        position,
        map,
        zIndex: 200,
        icon: {
          content: `<div class="highlight-marker">${idx + 1}</div>`,
          anchor: new naver.maps.Point(14, 14),
        },
      });
      const infoWindow = new naver.maps.InfoWindow({
        content: `<div style="padding:8px 10px;font-size:13px;max-width:220px;">
          <b>AI 추천 ${idx + 1}순위</b><br/>${b.address || "주소 정보 없음"}
          ${b.reason ? `<br/><span style="color:#2f7a4f">${b.reason}</span>` : ""}
        </div>`,
      });
      naver.maps.Event.addListener(marker, "click", () => infoWindow.open(map, marker));
      return marker;
    });

    map.fitBounds(fitBounds, { top: 60, right: 60, bottom: 60, left: 60 });
  }, [highlighted]);

  if (status === "no-client-id") {
    return (
      <div className="map-placeholder">
        <p>네이버 지도 Client ID가 설정되지 않았어요.</p>
        <p>
          <code>.env.local</code>의 <code>VITE_NAVER_MAP_CLIENT_ID</code> 값을
          네이버클라우드플랫폼 콘솔에서 발급받은 <b>Web Dynamic Map</b>용 Client ID로
          채워주세요.
        </p>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="map-placeholder">
        <p>지도를 불러오지 못했어요: {errorMessage}</p>
      </div>
    );
  }

  return (
    <div className="map-page">
      <div ref={mapRef} className="map-canvas" />
      <div className="map-sidebar">
        <div className="map-sidebar-chat">
          <MapChatPanel bounds={bounds} onRecommendations={setHighlighted} />
        </div>
        <div className="map-sidebar-buildings">
          <h3>주변 건물 ({buildings.length})</h3>
          <ul>
            {buildings.slice(0, 30).map((b) => (
              <li key={b.id}>
                {b.localBuildingName ?? b.officialBuildingName ?? `건물 #${b.id}`}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
