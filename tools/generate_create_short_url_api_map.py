import html
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GRAPH_PATH = ROOT / "graphify-out" / "graph.json"
OUTPUT_PATH = ROOT / "graphify-out" / "create-short-url-api-map.html"


graph = json.loads(GRAPH_PATH.read_text(encoding="utf-8"))
all_nodes = {n["id"]: n for n in graph.get("nodes", [])}
links = graph.get("links", graph.get("edges", []))

labels = {
    "UrlController",
    ".createShortUrl()",
    "PostMapping",
    "ResponseEntity",
    "HttpServletRequest",
    "UrlRequest",
    "UrlResponse",
    "RateLimitingService",
    ".getCreateUrlBucket()",
    "Bucket",
    "UrlService",
    ".fromRequest()",
    ".saveUrl()",
    ".toResponse()",
    ".getCurrentUser()",
    ".enrichUrlWithAI()",
    ".generateShortUrl()",
    ".findByShortUrl()",
    "UrlRepository",
    ".generateQrCodeBase64()",
    "QrCodeService",
    "CustomException",
    "Url",
}

selected = {n["id"] for n in graph.get("nodes", []) if n.get("label") in labels}
seed = next((n["id"] for n in graph.get("nodes", []) if n.get("label") == ".createShortUrl()"), None)
if seed:
    selected.add(seed)

focused_links = []
for edge in links:
    source = edge.get("source")
    target = edge.get("target")
    if (source in selected and target in selected) or source == seed or target == seed:
        if source in all_nodes and target in all_nodes:
            selected.add(source)
            selected.add(target)
            focused_links.append(edge)

groups = [
    "Controller / HTTP",
    "DTO / Entity",
    "Rate limiting",
    "Service flow",
    "Repository",
    "QR response",
    "Error path",
    "Other",
]
palette = ["#4E79A7", "#F28E2B", "#59A14F", "#E15759", "#76B7B2", "#EDC948", "#B07AA1", "#9CA3AF"]
colors = dict(zip(groups, palette))


def group_for(label: str) -> str:
    if label in {"UrlController", ".createShortUrl()", "PostMapping", "ResponseEntity", "HttpServletRequest"}:
        return "Controller / HTTP"
    if label in {"UrlRequest", "UrlResponse", "Url"}:
        return "DTO / Entity"
    if label in {"RateLimitingService", ".getCreateUrlBucket()", "Bucket"}:
        return "Rate limiting"
    if label in {
        "UrlService",
        ".fromRequest()",
        ".saveUrl()",
        ".toResponse()",
        ".getCurrentUser()",
        ".enrichUrlWithAI()",
        ".generateShortUrl()",
    }:
        return "Service flow"
    if label in {"UrlRepository", ".findByShortUrl()"}:
        return "Repository"
    if label in {"QrCodeService", ".generateQrCodeBase64()"}:
        return "QR response"
    if label == "CustomException":
        return "Error path"
    return "Other"


vis_nodes = []
for node_id in selected:
    node = all_nodes[node_id]
    label = node.get("label", node_id)
    group = group_for(label)
    title = "<br>".join(
        [
            f"<b>{html.escape(label)}</b>",
            f"Group: {html.escape(group)}",
            f"Source: {html.escape(node.get('source_file', ''))}",
            f"Location: {html.escape(node.get('source_location', ''))}",
        ]
    )
    vis_nodes.append(
        {
            "id": node_id,
            "label": label,
            "group": group,
            "color": colors[group],
            "title": title,
            "shape": "box" if label.startswith(".") else "dot",
            "size": 28 if label == ".createShortUrl()" else 18,
            "font": {"size": 18 if label == ".createShortUrl()" else 13, "color": "#f4f4f5"},
        }
    )

vis_edges = []
seen = set()
for edge in focused_links:
    source = edge.get("source")
    target = edge.get("target")
    key = (source, target, edge.get("relation"), edge.get("context"))
    if key in seen:
        continue
    seen.add(key)
    relation = edge.get("relation", "related")
    confidence = edge.get("confidence", "")
    vis_edges.append(
        {
            "from": source,
            "to": target,
            "label": relation,
            "title": (
                f"{html.escape(relation)}<br>{html.escape(str(confidence))}<br>"
                f"{html.escape(edge.get('source_file', ''))} {html.escape(edge.get('source_location', ''))}"
            ),
            "arrows": "to",
            "color": {"color": "#8b949e" if confidence == "EXTRACTED" else "#c9a227"},
            "font": {"align": "middle", "size": 11, "color": "#d4d4d8"},
            "dashes": confidence != "EXTRACTED",
        }
    )

legend = "".join(
    f'<div class="item"><span class="dot" style="background:{colors[group]}"></span>{html.escape(group)}</div>'
    for group in groups
    if any(node["group"] == group for node in vis_nodes)
)

html_doc = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Graphify API Map - UrlController.createShortUrl()</title>
<script src="https://unpkg.com/vis-network@9.1.6/standalone/umd/vis-network.min.js"></script>
<style>
  * {{ box-sizing: border-box; }}
  body {{ margin: 0; height: 100vh; background: #101114; color: #f4f4f5; font-family: Segoe UI, Arial, sans-serif; display: grid; grid-template-columns: 1fr 340px; }}
  #graph {{ min-width: 0; height: 100vh; }}
  aside {{ border-left: 1px solid #2f343c; background: #181a1f; padding: 18px; overflow: auto; }}
  h1 {{ font-size: 18px; margin: 0 0 8px; font-weight: 650; }}
  h2 {{ font-size: 13px; color: #a1a1aa; margin: 22px 0 8px; text-transform: uppercase; letter-spacing: .04em; }}
  p, li {{ font-size: 13px; line-height: 1.45; color: #d4d4d8; }}
  code {{ color: #f9fafb; background: #272b33; padding: 2px 5px; border-radius: 4px; }}
  .legend {{ display: grid; gap: 8px; }}
  .item {{ display: flex; align-items: center; gap: 8px; font-size: 13px; }}
  .dot {{ width: 12px; height: 12px; border-radius: 50%; flex: 0 0 12px; }}
  .meta {{ color: #a1a1aa; font-size: 12px; }}
  @media (max-width: 800px) {{ body {{ grid-template-columns: 1fr; grid-template-rows: 65vh auto; }} #graph {{ height: 65vh; }} aside {{ border-left: 0; border-top: 1px solid #2f343c; }} }}
</style>
</head>
<body>
<div id="graph"></div>
<aside>
  <h1>UrlController.createShortUrl()</h1>
  <p><code>POST /save</code> creates a short URL from <code>UrlRequest</code>, applies per-IP create rate limiting, saves through <code>UrlService</code>, and returns <code>UrlResponse</code>.</p>
  <p class="meta">Solid edges are extracted. Dashed edges are inferred by graphify.</p>
  <h2>Flow</h2>
  <ol>
    <li>Controller receives <code>UrlRequest</code> and <code>HttpServletRequest</code>.</li>
    <li>Rate limiter checks <code>getCreateUrlBucket(remoteAddr)</code>.</li>
    <li><code>fromRequest()</code> maps the request to <code>Url</code>.</li>
    <li><code>saveUrl()</code> handles user, password, AI safety, alias, expiry, clicks, and persistence.</li>
    <li><code>toResponse()</code> builds the response and QR code.</li>
  </ol>
  <h2>Legend</h2>
  <div class="legend">{legend}</div>
  <h2>Files</h2>
  <p><code>src/main/java/in/proofofconcept/url/shortner/controller/UrlController.java:97</code></p>
  <p><code>src/main/java/in/proofofconcept/url/shortner/service/UrlService.java:84</code></p>
</aside>
<script>
const nodes = new vis.DataSet({json.dumps(vis_nodes, ensure_ascii=False)});
const edges = new vis.DataSet({json.dumps(vis_edges, ensure_ascii=False)});
const network = new vis.Network(document.getElementById('graph'), {{nodes, edges}}, {{
  layout: {{ improvedLayout: true }},
  physics: {{ solver: 'forceAtlas2Based', forceAtlas2Based: {{ gravitationalConstant: -80, springLength: 160, springConstant: 0.08 }}, stabilization: {{ iterations: 160 }} }},
  interaction: {{ hover: true, tooltipDelay: 80, navigationButtons: true, keyboard: true }},
  nodes: {{ borderWidth: 1, margin: 10 }},
  edges: {{ smooth: {{ type: 'dynamic' }}, width: 1.5 }}
}});
network.once('stabilizationIterationsDone', () => network.fit({{ animation: true }}));
</script>
</body>
</html>
"""

OUTPUT_PATH.write_text(html_doc, encoding="utf-8")
print(f"{OUTPUT_PATH.relative_to(ROOT)}")
print(f"nodes={len(vis_nodes)} edges={len(vis_edges)}")
