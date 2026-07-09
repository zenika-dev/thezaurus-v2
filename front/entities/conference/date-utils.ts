import dayjs from "dayjs";
import "dayjs/locale/fr";
import type { ConferenceDate } from "./model";

dayjs.locale("fr");

export function formatConferenceDate(d: ConferenceDate): string {
    switch (d.type) {
        case "single":
            return dayjs(d.date).locale("fr").format("D MMMM YYYY");

        case "range": {
            const start = dayjs(d.start).locale("fr");
            const end = dayjs(d.end).locale("fr");

            if (start.year() !== end.year()) {
                return `${start.format("D MMMM YYYY")} - ${end.format("D MMMM YYYY")}`;
            }
            if (start.month() === end.month()) {
                return `${start.format("D")}-${end.format("D MMMM YYYY")}`;
            }
            return `${start.format("D MMMM")} - ${end.format("D MMMM YYYY")}`;
        }

        case "month": {
            return dayjs().locale("fr").year(d.year).month(d.month - 1).format("MMMM YYYY");
        }
    }
}

export function getConferenceYear(d: ConferenceDate): number {
    switch (d.type) {
        case "single":
            return dayjs(d.date).year();
        case "range":
            return dayjs(d.start).year();
        case "month":
            return d.year;
    }
}

export function getConferenceSortKey(d: ConferenceDate): string {
    switch (d.type) {
        case "single":
            return d.date;
        case "range":
            return d.start;
        case "month":
            return `${d.year}-${String(d.month).padStart(2, "0")}-01`;
    }
}
