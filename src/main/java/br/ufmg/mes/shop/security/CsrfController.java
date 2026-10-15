package br.ufmg.mes.shop.security;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CsrfResponse(token));
    }

    public static class CsrfResponse {

        private final String headerName;
        private final String token;

        public CsrfResponse(CsrfToken csrf) {
            this.headerName = csrf.getHeaderName();
            this.token = csrf.getToken();
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getToken() {
            return token;
        }
    }
}
