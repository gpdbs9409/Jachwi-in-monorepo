import { mainApi } from "./client";
import type { Building, MapBounds } from "./types";

export async function fetchBuildingsInArea(bounds: MapBounds): Promise<Building[]> {
  const { data } = await mainApi.get<Building[]>("/api/v1/map/view/position", {
    params: bounds,
  });
  return data;
}
