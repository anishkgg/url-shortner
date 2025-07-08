package in.proofofconcept.url.shortner.controller;
import in.proofofconcept.url.shortner.dto.UrlDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.proofofconcept.url.shortner.model.Url;
import in.proofofconcept.url.shortner.service.UrlService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/url/")
public class UrlController {

	private final UrlService urlService;

    @Autowired
    UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("all")
   public ResponseEntity<List<UrlDto>> getAllUrls() {
        return ResponseEntity.ok(urlService.getAllDto());
    }

    @GetMapping("get/{id}")
    public Url getOriginalUrlById(@PathVariable Long id) {
        return urlService.getOriginalUrlById(id);
    }

    @GetMapping("{shortUrl}")
    public ResponseEntity<UrlDto> getOriginalUrl(@PathVariable String shortUrl) {
        Url url = urlService.findByShortUrl(shortUrl);
        if(url != null) {
            return ResponseEntity.ok(urlService.toDto(url));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("save")
    public UrlDto createShortUrl(@RequestBody UrlDto urlDto) {
        Url url = urlService.fromDto(urlDto);
        Url savedUrl = urlService.saveUrl(url);
        return urlService.toDto(savedUrl);
    }

    @PutMapping("update/{id}")
    public Url updateUrl(@PathVariable Long id, @RequestBody Url updatedUrl) {
        return urlService.updateUrl(id, updatedUrl);
    }

    @DeleteMapping("delete/{id}")
    public String deleteUrl(@PathVariable Long id) {
        boolean delete = urlService.deleteUrl(id);
        if(!delete) {
            return "URL is not found with Id" + id;
        } else {
            return "Delete Successfully";
        }
    }

    @DeleteMapping("delete/all")
    public ResponseEntity<String> deleteAll() {
        urlService.deleteAllUrls();
        return ResponseEntity.ok("All Urls have been deleted");
    }

}
