---
type: "query"
date: "2026-05-31T19:49:27.827386+00:00"
question: "show getOriginalUrlById knowledge map"
contributor: "graphify"
source_nodes: [".getOriginalUrlById()", "UrlService", "UrlController", "UrlRepository"]
---

# Q: show getOriginalUrlById knowledge map

## Answer

Expanded from original query via graph vocab: [get, original, url]. Traversed .getOriginalUrlById(): service method in UrlService.java L140 returns urlRepository.findById(id).orElse(null); controller endpoint UrlController.java L43-L45 calls it for GET get/{id}; delete endpoint UrlController.java L128-L130 also calls it before deleting; related nodes include UrlService, UrlController, UrlRepository, Url, UrlResponse, Long, ResponseEntity.

## Source Nodes

- .getOriginalUrlById()
- UrlService
- UrlController
- UrlRepository