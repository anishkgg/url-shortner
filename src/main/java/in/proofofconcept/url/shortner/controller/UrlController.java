package in.proofofconcept.url.shortner.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.service.UrlService;

@RestController
@RequestMapping("/api")
public class UrlController {
	
	private final UrlService urlService;

    UrlController(UrlService urlService) {
        this.urlService = urlService;
    }
    @GetMapping("/{shortUrl}")
    public ResponseEntity<Url> getOriginalUrl(@PathVariable String shortUrl) {
        Url url = urlService.findByShortUrl(shortUrl);
        if (url != null) {
            return ResponseEntity.ok(url);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/shorten")
    public ResponseEntity<Url> createShortUrl(@RequestBody Url url) {
        Url savedUrl = urlService.saveUrl(url);
        return ResponseEntity.ok(savedUrl);
    }
}
