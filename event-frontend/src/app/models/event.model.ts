export interface Event {
  id?: number;
  titolo: string;
  descrizione: string;
  data: string;
  luogo: string;
  categoria: string;
  latitude?: number | null;
  longitude?: number | null;
}
