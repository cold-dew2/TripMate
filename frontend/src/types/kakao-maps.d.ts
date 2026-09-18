export {};

declare global {
  namespace kakao.maps {
    class LatLng {
      constructor(lat: number, lng: number);
      getLat(): number;
      getLng(): number;
    }

    interface MapOptions {
      center: LatLng;
      level?: number;
    }

    class Map {
      constructor(container: HTMLElement, options: MapOptions);
      setCenter(latlng: LatLng): void;
      setLevel(level: number): void;
    }

    interface MarkerOptions {
      position: LatLng;
      map?: Map;
    }

    class Marker {
      constructor(options: MarkerOptions);
      setMap(map: Map | null): void;
    }

    function load(callback: () => void): void;

    namespace services {
      type Status = "OK" | "ZERO_RESULT" | "ERROR";

      interface AddressSearchResult {
        x: string;
        y: string;
        address_name: string;
      }

      class Geocoder {
        addressSearch(
          address: string,
          callback: (result: AddressSearchResult[], status: Status) => void
        ): void;
      }
    }
  }

  interface Window {
    kakao: typeof kakao;
  }
}
