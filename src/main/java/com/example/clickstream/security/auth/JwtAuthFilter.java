//package com.example.clickstream.security.auth;
//
//import com.example.clickstream.security.service.JwtTokenService;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//
//@Component
//public class JwtAuthFilter extends OncePerRequestFilter {
//    private final JwtTokenService jwtTokenService;
//    private final UserDetailsService userDetailsService;
//
//    public JwtAuthFilter(
//            JwtTokenService jwtTokenService,
//            @Lazy UserDetailsService userDetailsService
//    ) {
//        this.jwtTokenService = jwtTokenService;
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest request,
//            @NotNull HttpServletResponse response,
//            @NotNull FilterChain filterChain
//    ) throws ServletException, IOException {
//        final String authHeader = request.getHeader("Authorization");
//        String username = null;
//        String jwtToken = null;
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            jwtToken = authHeader.substring(7);
//            try {
//                username = jwtTokenService.extractUsername(jwtToken);
//            } catch (Exception e) {
//                logger.warn("Invalid JWT Token: " + jwtToken);
//            }
//        }
//
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//
//            if (jwtTokenService.validateToken(jwtToken, userDetails)) {
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.getAuthorities()
//                );
//                authentication.setDetails(authentication.getDetails());
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//}
