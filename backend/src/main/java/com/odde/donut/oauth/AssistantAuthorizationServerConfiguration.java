package com.odde.donut.oauth;

import java.util.List;
import java.util.Set;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.web.OAuth2ProtectedResourceMetadataFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;

@Configuration
public class AssistantAuthorizationServerConfiguration {
  @Bean
  AuthorizationServerSettings authorizationServerSettings(AssistantOAuthEndpoints endpoints) {
    return AuthorizationServerSettings.builder()
        .issuer(endpoints.issuer())
        .authorizationEndpoint(AssistantOAuthEndpoints.AUTHORIZE)
        .tokenEndpoint(AssistantOAuthEndpoints.TOKEN)
        .tokenRevocationEndpoint(AssistantOAuthEndpoints.REVOKE)
        .tokenIntrospectionEndpoint(AssistantOAuthEndpoints.INTROSPECT)
        .pushedAuthorizationRequestEndpoint("/api/oauth/par")
        .deviceAuthorizationEndpoint("/api/oauth/device-authorization")
        .deviceVerificationEndpoint("/api/oauth/device-verification")
        .jwkSetEndpoint("/api/oauth/jwks")
        .clientRegistrationEndpoint("/api/oauth/register")
        .build();
  }

  @Bean
  @Order(1)
  SecurityFilterChain assistantAuthorizationChain(
      HttpSecurity http,
      AssistantAuthorizationRequestValidator validator,
      AssistantGrantService grants)
      throws Exception {
    OAuth2AuthorizationServerConfigurer server = new OAuth2AuthorizationServerConfigurer();
    server.withObjectPostProcessor(
        new ObjectPostProcessor<AuthenticationProvider>() {
          @Override
          @SuppressWarnings("unchecked")
          public <O extends AuthenticationProvider> O postProcess(O provider) {
            // Keep the original list entries visible to SAS's shared request validator.
            if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider
                || provider instanceof OAuth2AuthorizationConsentAuthenticationProvider) {
              return (O) new AssistantGrantAuthenticationProvider(provider, grants);
            }
            return provider;
          }
        });
    var jsonContexts =
        new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults()
                .matcher(AssistantOAuthEndpoints.CONSENT_CONTEXT),
            PathPatternRequestMatcher.withDefaults()
                .matcher(AssistantOAuthEndpoints.ASSISTANTS + "/**"));
    var entryPoint =
        new DelegatingAuthenticationEntryPoint(
            new LoginUrlAuthenticationEntryPoint("/login"),
            List.of(
                new RequestMatcherEntry<>(
                    jsonContexts, new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))));
    http.securityMatcher(new OrRequestMatcher(server.getEndpointsMatcher(), jsonContexts))
        .with(
            server,
            config ->
                config
                    .clientAuthentication(
                        endpoint ->
                            endpoint.authenticationProviders(
                                providers ->
                                    providers.forEach(
                                        provider -> {
                                          if (provider
                                              instanceof
                                              ClientSecretAuthenticationProvider secret) {
                                            secret.setPasswordEncoder(
                                                PasswordEncoderFactories
                                                    .createDelegatingPasswordEncoder());
                                          }
                                        })))
                    .authorizationEndpoint(
                        endpoint ->
                            endpoint
                                .consentPage("/assistant-consent")
                                .authenticationProviders(
                                    providers ->
                                        providers.forEach(
                                            provider -> {
                                              if (provider
                                                  instanceof
                                                  OAuth2AuthorizationCodeRequestAuthenticationProvider
                                                      codeRequest) {
                                                codeRequest.setAuthenticationValidator(validator);
                                              }
                                            })))
                    .tokenEndpoint(
                        endpoint ->
                            endpoint.authenticationProviders(
                                providers ->
                                    AssistantGrantAuthenticationProvider.serialize(
                                        providers, grants)))
                    .tokenRevocationEndpoint(
                        endpoint ->
                            endpoint.authenticationProviders(
                                providers ->
                                    AssistantGrantAuthenticationProvider.serialize(
                                        providers, grants)))
                    .authorizationServerMetadataEndpoint(
                        metadata ->
                            metadata.authorizationServerMetadataCustomizer(
                                builder ->
                                    builder
                                        .claims(
                                            claims -> {
                                              claims
                                                  .keySet()
                                                  .retainAll(
                                                      Set.of(
                                                          "issuer",
                                                          "authorization_endpoint",
                                                          "token_endpoint",
                                                          "revocation_endpoint",
                                                          "response_types_supported"));
                                              claims.put(
                                                  "scopes_supported",
                                                  List.of(
                                                      AssistantAuthorizationConfiguration
                                                          .READ_SCOPE,
                                                      AssistantAuthorizationConfiguration
                                                          .RECORD_SCOPE));
                                              claims.put(
                                                  "grant_types_supported",
                                                  List.of("authorization_code", "refresh_token"));
                                              claims.put(
                                                  "token_endpoint_auth_methods_supported",
                                                  List.of("client_secret_basic"));
                                              claims.put(
                                                  "revocation_endpoint_auth_methods_supported",
                                                  List.of("client_secret_basic"));
                                              claims.put(
                                                  "code_challenge_methods_supported",
                                                  List.of("S256"));
                                            })
                                        .tlsClientCertificateBoundAccessTokens(false))))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(AssistantOAuthEndpoints.ISSUER_METADATA)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(new CookieCsrfTokenRepository())
                    .withObjectPostProcessor(
                        new ObjectPostProcessor<CsrfFilter>() {
                          @Override
                          public <O extends CsrfFilter> O postProcess(O filter) {
                            // SAS adds an endpoint-wide exemption during init. Consent is a
                            // browser form, so install its precise policy on the final filter.
                            filter.setRequireCsrfProtectionMatcher(
                                new OrRequestMatcher(
                                    PathPatternRequestMatcher.withDefaults()
                                        .matcher(
                                            HttpMethod.POST, AssistantOAuthEndpoints.AUTHORIZE),
                                    PathPatternRequestMatcher.withDefaults()
                                        .matcher(
                                            HttpMethod.DELETE, AssistantOAuthEndpoints.ASSISTANT)));
                            return filter;
                          }
                        }))
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
    return http.build();
  }

  @Bean
  FilterRegistrationBean<OAuth2ProtectedResourceMetadataFilter> assistantProtectedResourceMetadata(
      AssistantOAuthEndpoints endpoints) {
    OAuth2ProtectedResourceMetadataFilter filter = new OAuth2ProtectedResourceMetadataFilter();
    filter.setProtectedResourceMetadataCustomizer(
        metadata ->
            metadata
                .resource(endpoints.resource())
                .authorizationServer(endpoints.issuer())
                .scope(AssistantAuthorizationConfiguration.READ_SCOPE)
                .scope(AssistantAuthorizationConfiguration.RECORD_SCOPE)
                .tlsClientCertificateBoundAccessTokens(false));
    FilterRegistrationBean<OAuth2ProtectedResourceMetadataFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.addUrlPatterns(AssistantOAuthEndpoints.RESOURCE_METADATA);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
