#!/usr/bin/env python3
"""Piccolo aiuto per leggere lo schermo dell'emulatore (file XML di uiautomator).

  ui.py has  FILE "testo1|testo2"   -> esce con 0 se compare uno dei testi (o una descrizione)
  ui.py tap  FILE "testo"           -> stampa "x y" del centro del primo elemento con quel testo
"""
import re
import sys
import xml.etree.ElementTree as ET


def load(path):
    try:
        return list(ET.parse(path).iter("node"))
    except Exception:
        return []


def matches(node, wanted):
    return node.get("text") == wanted or node.get("content-desc") == wanted


def center(bounds):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds or "")
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def main():
    cmd, path, arg = sys.argv[1], sys.argv[2], sys.argv[3]
    nodes = load(path)
    if cmd == "has":
        wanted = arg.split("|")
        sys.exit(0 if any(matches(n, w) for n in nodes for w in wanted) else 1)
    if cmd == "tap":
        for n in nodes:
            if matches(n, arg):
                c = center(n.get("bounds"))
                if c:
                    print(c[0], c[1])
                    sys.exit(0)
        sys.exit(1)
    sys.exit(2)


main()
