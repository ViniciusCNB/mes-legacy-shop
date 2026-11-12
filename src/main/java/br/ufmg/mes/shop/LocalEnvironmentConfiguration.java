package br.ufmg.mes.shop;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Profile("local")
@PropertySource(value = "file:./.env", ignoreResourceNotFound = true)
public class LocalEnvironmentConfiguration {
}
