package com.basewebapp.platform.ai;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration dello starter: basta la dipendenza sul classpath perché l'applicazione trovi
 * un {@link InferenceProviderRegistry} pronto, popolato dai provider dichiarati in
 * {@code platform.ai.*} e dai bean {@link InferenceProvider} dell'applicazione stessa.
 *
 * <p>Si disattiva tutto con {@code platform.ai.enabled=false}. Il registry è
 * {@link ConditionalOnMissingBean}: un'applicazione può sostituirlo con il proprio.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(AiMlProperties.class)
@ConditionalOnProperty(prefix = "platform.ai", name = "enabled", matchIfMissing = true)
public class AiMlIntegrationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InferenceProviderRegistry inferenceProviderRegistry(
        AiMlProperties properties,
        ObjectProvider<InferenceProvider> applicationProviders,
        ObjectProvider<RestClient.Builder> restClientBuilder
    ) {
        List<InferenceProvider> providers = new ArrayList<>(applicationProviders.orderedStream().toList());
        properties
            .getRest()
            .forEach((name, rest) ->
                providers.add(new RestInferenceProvider(name, rest, restClientBuilder.getIfAvailable(RestClient::builder)))
            );
        if (properties.getMock().isEnabled()) {
            providers.add(new MockInferenceProvider(properties.getMock()));
        }
        return new InferenceProviderRegistry(providers, properties.getDefaultProvider());
    }
}
