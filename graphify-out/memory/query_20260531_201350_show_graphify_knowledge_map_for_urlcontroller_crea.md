---
type: "query"
date: "2026-05-31T20:13:50.306244+00:00"
question: "show graphify knowledge map for UrlController.createShortUrl()"
contributor: "graphify"
source_nodes: [".createShortUrl()", "UrlController", "UrlService", ".getCreateUrlBucket()", ".fromRequest()", ".saveUrl()", ".toResponse()", "UrlRepository"]
---

# Q: show graphify knowledge map for UrlController.createShortUrl()

## Answer

Expanded from original query via graph vocab: [create, short, url]. UrlController.createShortUrl at UrlController.java L97 handles POST save, accepts UrlRequest and HttpServletRequest, gets create rate-limit bucket by remote IP at L100, throws CustomException on limit exhaustion at L102, maps UrlRequest to Url via UrlService.fromRequest at L105, persists via UrlService.saveUrl at L106, returns ResponseEntity.ok(UrlService.toResponse(savedUrl)) at L107. saveUrl associates current user, hashes password, enriches with AI, rejects unsafe URL, generates or validates shortUrl, defaults expiry, sets clicks 0, and saves with UrlRepository.save. Response includes id originalUrl shortUrl expiryDate clicks qrCodeBase64 isPasswordProtected isOneTimeUse summary category isSafe.

## Source Nodes

- .createShortUrl()
- UrlController
- UrlService
- .getCreateUrlBucket()
- .fromRequest()
- .saveUrl()
- .toResponse()
- UrlRepository