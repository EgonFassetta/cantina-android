#!/usr/bin/env python3
"""Statistiche del catalogo letto dal database (solo per capire se i dati influiscono sulle prove)."""
import json
import sys

w = json.load(sys.stdin)
avail = [x for x in w if str(x.get("disponibile_vendita")).lower() in ("true", "t", "1") and float(x.get("quantita") or 0) > 0]
img = [x for x in w if x.get("immagine_url")]
print("catalogo:", len(w), "vini | disponibili:", len(avail), "| con foto:", len(img))
if avail:
    print("primo disponibile:", avail[0].get("cantina"), "-", avail[0].get("nome"), "| foto:", bool(avail[0].get("immagine_url")))
