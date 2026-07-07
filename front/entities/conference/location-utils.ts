import { ConferenceLocation } from "./model";

export interface OpenStreetMapLocation {
  display_name?: string;
  address?: {
    city?: string;
    town?: string;
    village?: string;
    municipality?: string;
    country?: string;
    postcode?: string;
  };
}

export function formatLocation(location: ConferenceLocation | string | undefined | null): string {
  if (!location) return "";

  const removePostalCode = (str: string) =>
    str.replace(/\b\d{4,5}\b/g, '').replace(/\s+/g, ' ').replace(/,\s*,/g, ',').replace(/(^,\s*)|(\s*,$)/g, '').trim();

  if (typeof location === 'string') {
    return removePostalCode(location);
  }

  if (location.city && location.country) {
    return `${location.city}, ${location.country}`;
  }

  if (location.city) return location.city;
  if (location.country) return location.country;

  if (location.address) {
    return removePostalCode(location.address);
  }

  return "";
}

export function parseLocation(locationData: OpenStreetMapLocation | string): ConferenceLocation {
  if (!locationData) return { address: "" };

  if (typeof locationData === "string") {
    return { address: locationData };
  }

  const addr = locationData.address;
  if (addr) {
    return {
      address: locationData.display_name || "",
      city: addr.city || addr.town || addr.village || addr.municipality || "",
      country: addr.country || "",
      postalCode: addr.postcode || "",
    };
  }

  return { address: locationData.display_name || "" };
}