package com.aether.ms_calculator.calculator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CalculatorJsonNodeWebConfiguration implements WebMvcConfigurer {

  private final ObjectMapper objectMapper;

  public CalculatorJsonNodeWebConfiguration(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    builder.addCustomConverter(new Jackson2JsonNodeHttpMessageConverter(objectMapper));
  }

  private static final class Jackson2JsonNodeHttpMessageConverter implements HttpMessageConverter<JsonNode> {

    private static final List<MediaType> MEDIA_TYPES =
        List.of(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));

    private final ObjectMapper objectMapper;

    private Jackson2JsonNodeHttpMessageConverter(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
      return JsonNode.class.isAssignableFrom(clazz) && compatible(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
      return JsonNode.class.isAssignableFrom(clazz) && compatible(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
      return MEDIA_TYPES;
    }

    @Override
    public JsonNode read(Class<? extends JsonNode> clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
      return objectMapper.readTree(inputMessage.getBody());
    }

    @Override
    public void write(JsonNode node, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
      if (outputMessage.getHeaders().getContentType() == null) {
        outputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      }
      objectMapper.writeValue(outputMessage.getBody(), node);
    }

    private static boolean compatible(MediaType mediaType) {
      return mediaType == null || MEDIA_TYPES.stream().anyMatch(supported -> supported.isCompatibleWith(mediaType));
    }
  }
}
