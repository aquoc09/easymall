package com.quocnva.easymall.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quocnva.easymall.dtos.request.product.ProductCreateRequest;
import com.quocnva.easymall.dtos.request.product.ProductImageRequest;
import com.quocnva.easymall.dtos.request.product.ProductUpdateRequest;
import com.quocnva.easymall.dtos.request.product.ProductVariantRequest;
import com.quocnva.easymall.dtos.response.product.ProductImageResponse;
import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.dtos.response.product.ProductVariantResponse;
import com.quocnva.easymall.entity.ProductEntity;
import com.quocnva.easymall.entity.ProductImageEntity;
import com.quocnva.easymall.entity.ProductVariantEntity;
import com.quocnva.easymall.enums.TargetGender;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * ProductMapper — MapStruct cho các simple fields.
 * Complex fields (JSONB → Java types) xử lý thủ công trong @AfterMapping.
 *
 * <p>componentModel = "spring" → MapStruct generate @Component, tương thích DI.
 * uses = {} → inject ObjectMapper qua @Autowired trong abstract class.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class ProductMapper {

    // Inject ObjectMapper để parse/serialize JSONB fields
    @Autowired
    protected ObjectMapper objectMapper;

    // S3 base URL — đọc từ application.yaml: storage.base-url
    @Value("${storage.base-url}")
    protected String storageBaseUrl;

    // ══════════════════════════════════════════════════════════════════
    // ProductEntity ← ProductCreateRequest
    // ══════════════════════════════════════════════════════════════════

    /**
     * MapStruct map các simple fields tự động.
     * JSONB fields (optionsConfig, productTags) được xử lý trong @AfterMapping.
     * variants và images bị ignore ở đây — service tự xử lý.
     */
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "productSlug", ignore = true)   // service sinh slug
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "searchVector", ignore = true)
    @Mapping(target = "variants", ignore = true)       // service xử lý
    @Mapping(target = "images", ignore = true)         // service xử lý
    @Mapping(target = "optionsConfig", ignore = true)  // @AfterMapping
    @Mapping(target = "productTags", ignore = true)    // @AfterMapping
    @Mapping(target = "inStock", ignore = true)        // default true từ @PrePersist
    @Mapping(target = "targetGender", expression = "java(toShort(request.getTargetGender()))")
    public abstract ProductEntity toEntity(ProductCreateRequest request);

    @AfterMapping
    protected void afterToEntity(ProductCreateRequest request, @MappingTarget ProductEntity entity) {
        // Strip extra JSON quotes do FE double-serialize (ví dụ: "\"Tên SP\"" → "Tên SP")
        // productName và productDescription được xử lý qua @Mapping(qualifiedByName) ở trên

        // productTags: List<String> → JSON string
        if (request.getProductTags() != null) {
            entity.setProductTags(toJsonString(request.getProductTags()));
        }
        // optionsConfig: Map<String, Object> -> JSON string
        if (request.getOptionsConfig() != null) {
            entity.setOptionsConfig(toJsonString(request.getOptionsConfig()));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // ProductEntity ← ProductUpdateRequest  (partial update / PATCH)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Cập nhật in-place các simple fields từ request vào entity đã tồn tại.
     * MapStruct tự bỏ qua các field null nhờ NullValuePropertyMappingStrategy.IGNORE.
     * JSONB fields (productTags, optionsConfig) và các field do service quản lý
     * được ignore ở đây — xử lý riêng trong @AfterMapping bên dưới.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "productId",     ignore = true)
    @Mapping(target = "productSlug",   ignore = true)   // giữ slug hiện tại, không đổi khi update
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "searchVector",  ignore = true)
    @Mapping(target = "variants",      ignore = true)   // service xử lý
    @Mapping(target = "images",        ignore = true)   // service xử lý
    @Mapping(target = "categoryId",    ignore = true)   // service validate rồi set
    @Mapping(target = "optionsConfig", ignore = true)   // @AfterMapping
    @Mapping(target = "productTags",   ignore = true)   // @AfterMapping
    @Mapping(target = "targetGender",  expression = "java(toShort(request.getTargetGender()))")
    public abstract void updateEntityFromRequest(ProductUpdateRequest request,
                                                @MappingTarget ProductEntity entity);

    @AfterMapping
    protected void afterUpdateEntity(ProductUpdateRequest request, @MappingTarget ProductEntity entity) {
        // Strip extra JSON quotes do FE double-serialize
        entity.setProductName(stripJsonQuotes(entity.getProductName()));
        entity.setProductDescription(stripJsonQuotes(entity.getProductDescription()));

        // productTags: List<String> → JSON string
        if (request.getProductTags() != null) {
            entity.setProductTags(toJsonString(request.getProductTags()));
        }
        // optionsConfig: Map<String, Object> -> JSON string
        if (request.getOptionsConfig() != null) {
            entity.setOptionsConfig(toJsonString(request.getOptionsConfig()));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // ProductEntity → ProductResponse
    // ══════════════════════════════════════════════════════════════════

    @Mapping(target = "optionsConfig", ignore = true)  // @AfterMapping
    @Mapping(target = "productTags", ignore = true)    // @AfterMapping
    @Mapping(target = "variants", ignore = true)       // mapper xử lý riêng
    @Mapping(target = "images", ignore = true)         // mapper xử lý riêng
    @Mapping(target = "targetGender", expression = "java(toTargetGender(entity.getTargetGender()))")
    public abstract ProductResponse toResponse(ProductEntity entity);

    @AfterMapping
    protected void afterToResponse(ProductEntity entity, @MappingTarget ProductResponse response) {
        // Strip extra quotes do data cũ trong DB hoặc FE double-serialize
        response.setProductSlug(stripJsonQuotes(response.getProductSlug()));
        response.setProductName(stripJsonQuotes(response.getProductName()));
        response.setProductDescription(stripJsonQuotes(response.getProductDescription()));

        // productTags: JSON string → List<String>
        if (entity.getProductTags() != null) {
            response.setProductTags(fromJsonList(entity.getProductTags()));
        }
        // optionsConfig: JSON string → Map<String, Object>
        if (entity.getOptionsConfig() != null) {
            response.setOptionsConfig(fromJsonMap(entity.getOptionsConfig()));
        }
        // variants — map rồi deserialize variantAttributes
        if (entity.getVariants() != null) {
            response.setVariants(toVariantResponseList(entity.getVariants()));
        }
        // images — prepend S3 base URL vào imageUrl
        if (entity.getImages() != null) {
            response.setImages(
                entity.getImages().stream()
                    .map(img -> {
                        ProductImageResponse r = toImageResponse(img);
                        if (img.getImageUrl() != null && !img.getImageUrl().startsWith("http")) {
                            r.setImageUrl(storageBaseUrl + "/" + img.getImageUrl());
                        }
                        return r;
                    })
                    .toList()
            );
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // ProductVariantEntity ← ProductVariantRequest
    // ══════════════════════════════════════════════════════════════════

    @Mapping(target = "variantId", ignore = true)
    @Mapping(target = "product", ignore = true)        // service set
    @Mapping(target = "isActive", ignore = true)       // default true
    @Mapping(target = "lockedStock", ignore = true)    // default 0
    @Mapping(target = "inventoryTransactions", ignore = true)
    @Mapping(target = "skuCode", ignore = true)           // service sinh SKU
    public abstract ProductVariantEntity toVariantEntity(ProductVariantRequest request);

    // ══════════════════════════════════════════════════════════════════
    // ProductVariantEntity → ProductVariantResponse
    // ══════════════════════════════════════════════════════════════════

    public abstract ProductVariantResponse toVariantResponse(ProductVariantEntity entity);

    @AfterMapping
    protected void afterToVariantResponse(ProductVariantEntity entity,
                                          @MappingTarget ProductVariantResponse response) {
        // Strip extra quotes khỏi skuCode (data cũ hoặc FE double-serialize)
        response.setSkuCode(stripJsonQuotes(response.getSkuCode()));

        // Prepend base URL nếu variantImage là S3 key (không phải full URL)
        String img = stripJsonQuotes(entity.getVariantImage());
        if (img != null && !img.startsWith("http")) {
            response.setVariantImage(storageBaseUrl + "/" + img);
        } else {
            response.setVariantImage(img);
        }
    }

    public abstract List<ProductVariantResponse> toVariantResponseList(List<ProductVariantEntity> entities);

    // ══════════════════════════════════════════════════════════════════
    // ProductImageEntity ← ProductImageRequest
    // ══════════════════════════════════════════════════════════════════

    @Mapping(target = "imageId", ignore = true)
    @Mapping(target = "product", ignore = true)
    public abstract ProductImageEntity toImageEntity(ProductImageRequest request);

    // ══════════════════════════════════════════════════════════════════
    // ProductImageEntity → ProductImageResponse
    // ══════════════════════════════════════════════════════════════════

    public abstract ProductImageResponse toImageResponse(ProductImageEntity entity);

    public abstract List<ProductImageResponse> toImageResponseList(List<ProductImageEntity> entities);

    // ══════════════════════════════════════════════════════════════════
    // Helper: JSON ↔ Java — thủ công, không phụ thuộc MapStruct
    // ══════════════════════════════════════════════════════════════════

    @Named("toJsonString")
    public String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INVALID_JSONB_FORMAT);
        }
    }

    /**
     * Strip leading/trailing JSON double-quotes do FE vô tình double-serialize.
     * Đánh dấu @Named để MapStruct chỉ dùng khi được chỉ định rõ bằng qualifiedByName,
     * tránh bị auto-pick làm global String converter.
     * Ví dụ: "\"hello\"" → "hello"; "hello" → "hello" (giữ nguyên nếu không có quotes).
     */
    @Named("stripJsonQuotes")
    public String stripJsonQuotes(String value) {
        if (value == null) return null;
        String v = value.trim();
        // Xử lý trường hợp nhiều lớp quote lồng nhau
        while (v.length() >= 2 && v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
            String unescaped = v.substring(1, v.length() - 1).replace("\\\"", "\"");
            // Nếu sau khi unwrap vẫn còn chứa escaped quotes thì tiếp tục unwrap
            v = unescaped;
        }
        return v;
    }

    public void validateJsonString(String json) {
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INVALID_JSONB_FORMAT);
        }
    }

    public List<String> fromJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public Map<String, Object> fromJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    public Map<String, String> fromJsonStringMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    /**
     * Convert TargetGender enum → Short (SMALLINT) khi lưu vào entity.
     * Null-safe.
     */
    public Short toShort(TargetGender gender) {
        return gender != null ? gender.getCode() : null;
    }

    /**
     * Convert Short (SMALLINT từ DB) → TargetGender enum khi map sang response.
     * Null-safe.
     */
    public TargetGender toTargetGender(Short code) {
        if (code == null) return null;
        return TargetGender.fromValue(code.intValue());
    }
}
