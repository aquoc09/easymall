package com.quocnva.easymall.service.product.impl;

import com.quocnva.easymall.config.AwsS3Properties;
import com.quocnva.easymall.dtos.request.product.ProductCreateRequest;
import com.quocnva.easymall.dtos.request.product.ProductImageRequest;
import com.quocnva.easymall.dtos.request.product.ProductUpdateRequest;
import com.quocnva.easymall.dtos.request.product.ProductVariantRequest;
import com.quocnva.easymall.dtos.response.product.ProductResponse;
import com.quocnva.easymall.dtos.response.product.ProductVariantResponse;
import com.quocnva.easymall.entity.ProductEntity;
import com.quocnva.easymall.entity.ProductImageEntity;
import com.quocnva.easymall.entity.ProductVariantEntity;

import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.ProductMapper;
import com.quocnva.easymall.repository.CategoryRepository;
import com.quocnva.easymall.repository.OrderDetailRepository;
import com.quocnva.easymall.repository.ProductImageRepository;
import com.quocnva.easymall.repository.ProductRepository;
import com.quocnva.easymall.repository.ProductVariantRepository;
import com.quocnva.easymall.repository.TempUploadRepository;
import com.quocnva.easymall.service.product.ProductService;
import com.quocnva.easymall.util.SkuGenerator;
import com.quocnva.easymall.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final TempUploadRepository tempUploadRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AwsS3Properties awsS3Properties;

    // ══════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 1. Validate category if provided
        if (request.getCategoryId() != null) {
            boolean categoryExists = categoryRepository.existsById(request.getCategoryId());
            if (!categoryExists) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND_FOR_PRODUCT);
            }
        }

        // 2. Map base product fields
        ProductEntity product = productMapper.toEntity(request);

        // 3. Sinh slug từ tên sản phẩm; ensure uniqueness
        String baseSlug = SlugUtils.toSlug(request.getProductName());
        String slug = ensureUniqueSlug(baseSlug);
        product.setProductSlug(slug);

        // 4. Lưu product trước để có productId (cần cho SKU generation)
        product = productRepository.save(product);

        // 5. Xử lý variants
        buildAndSaveVariants(request.getVariants(), product, null);

        // 6. Xử lý images
        if (request.getImages() != null) {
            buildAndSaveImages(request.getImages(), product);
        }

        // 7. Reload để lấy full data với variants & images
        product = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(product);
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Validate category if being changed
        if (request.getCategoryId() != null) {
            boolean categoryExists = categoryRepository.existsById(request.getCategoryId());
            if (!categoryExists) {
                throw new AppException(ErrorCode.CATEGORY_NOT_FOUND_FOR_PRODUCT);
            }
            product.setCategoryId(request.getCategoryId());
        }

        // Delegate null-safe field patching sang MapStruct (JSONB + variants + images bị ignore,
        // xử lý riêng bên dưới)
        productMapper.updateEntityFromRequest(request, product);

        // JSONB fields — @AfterMapping trong mapper đã xử lý khi gọi updateEntityFromRequest(),
        // nhưng để rõ ý định giữ nguyên logic variants/images riêng biệt ta vẫn tách ở đây.
        // (Không cần if-block nữa — mapper tự skip null)

        // Variants — update existing, create new, soft-delete missing
        if (request.getVariants() != null) {
            buildAndSaveVariants(request.getVariants(), product, productId);
        }

        // Images — thay thế toàn bộ
        if (request.getImages() != null) {
            product.getImages().clear();
            productRepository.save(product);
            buildAndSaveImages(request.getImages(), product);
        }

        product = productRepository.save(product);

        // Reload
        product = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(product);
    }

    // ══════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        ProductEntity product = productRepository.findByProductSlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════
    // GET VARIANTS BY PRODUCT ID
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return productMapper.toVariantResponseList(
                productVariantRepository.findAllByProductProductId(productId)
        );
    }

    // ══════════════════════════════════════════════════════════════════
    // DELETE (soft delete — set inStock = false)
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Soft delete: ẩn sản phẩm khỏi storefront
        product.setInStock(false);
        product.setInPopular(false);
        // Deactivate tất cả variants
        product.getVariants().forEach(v -> v.setIsActive(false));
        productRepository.save(product);
    }

    // ══════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════

    /**
     * Sinh slug duy nhất.
     * Nếu slug đã tồn tại → thêm suffix "-2", "-3", ... cho đến khi unique.
     */
    private String ensureUniqueSlug(String baseSlug) {
        if (!productRepository.existsByProductSlug(baseSlug)) {
            return baseSlug;
        }
        int counter = 2;
        while (productRepository.existsByProductSlug(baseSlug + "-" + counter)) {
            counter++;
        }
        return baseSlug + "-" + counter;
    }

    /**
     * Build và lưu variants từ request list.
     * Tự sinh SKU nếu request không cung cấp.
     *
     * @param variantRequests list variant request
     * @param product         product entity đã được save (có productId)
     * @param productIdForSku dùng cho SKU generation (null khi create thì dùng product.productId)
     */
    private void buildAndSaveVariants(
            List<ProductVariantRequest> variantRequests,
            ProductEntity product,
            Long productIdForSku
    ) {
        if (variantRequests == null || variantRequests.isEmpty()) return;

        Long idForSku = productIdForSku != null ? productIdForSku : product.getProductId();
        
        java.util.Set<String> requestSkus = new java.util.HashSet<>();

        for (ProductVariantRequest varReq : variantRequests) {
            // Sinh SKU
            String sku;
            String rawSku = varReq.getSkuCode();
            // Strip dấu ngoặc kép thừa
            if (rawSku != null) {
                rawSku = rawSku.replaceAll("^\"+|\"+$", "").trim();
            }
            if (rawSku != null && !rawSku.isBlank()) {
                sku = rawSku.toUpperCase();
            } else {
                String catCode = resolveCategoryCode(product.getCategoryId());
                boolean isSimpleVariant = varReq.getVariantAttributes() == null
                        || varReq.getVariantAttributes().isEmpty();
                if (isSimpleVariant) {
                    sku = catCode + "-" + idForSku + "-DEFAULT";
                } else {
                    sku = SkuGenerator.generate(catCode, idForSku, varReq.getVariantAttributes());
                }
            }
            requestSkus.add(sku);

            // Tìm variant đã tồn tại trong product (update mode) bằng VariantAttributes thay vì SKU
            ProductVariantEntity existingVariant = product.getVariants().stream()
                    .filter(v -> {
                        java.util.Map<String, String> reqAttrs = varReq.getVariantAttributes();
                        java.util.Map<String, String> dbAttrs = v.getVariantAttributes();
                        boolean reqEmpty = (reqAttrs == null || reqAttrs.isEmpty());
                        boolean dbEmpty = (dbAttrs == null || dbAttrs.isEmpty());
                        if (reqEmpty && dbEmpty) return true;
                        if (!reqEmpty && !dbEmpty) return reqAttrs.equals(dbAttrs);
                        return false;
                    })
                    .findFirst()
                    .orElse(null);

            if (existingVariant != null) {
                // Update properties
                existingVariant.setPrice(varReq.getPrice());
                existingVariant.setCostPrice(varReq.getCostPrice());
                // Cập nhật lại SKU phòng trường hợp category code thay đổi làm SKU bị lệch
                existingVariant.setSkuCode(sku);

                if (varReq.getVariantAttributes() != null) {
                    existingVariant.setVariantAttributes(varReq.getVariantAttributes());
                }
                if (varReq.getStockQuantity() != null) {
                    existingVariant.setStockQuantity(varReq.getStockQuantity());
                }
                existingVariant.setIsActive(true); // khôi phục nếu từng bị soft delete

                // Image handling
                String rawImageUrl = varReq.getVariantImage();
                if (rawImageUrl != null && !rawImageUrl.isBlank()) {
                    rawImageUrl = rawImageUrl.replaceAll("^\"+|\"+$", "").trim();
                    String s3Key = rawImageUrl.startsWith("http")
                            ? rawImageUrl.replaceFirst(awsS3Properties.getBaseUrl() + "/?", "")
                            : rawImageUrl;
                    existingVariant.setVariantImage(s3Key);
                    tempUploadRepository.deleteByUrl(rawImageUrl.startsWith("http")
                            ? rawImageUrl
                            : awsS3Properties.getBaseUrl() + "/" + s3Key);
                }
            } else {
                // Create new
                if (productVariantRepository.existsBySkuCode(sku)) {
                    throw new AppException(ErrorCode.SKU_ALREADY_EXISTS);
                }
                ProductVariantEntity variant = productMapper.toVariantEntity(varReq);
                variant.setProduct(product);
                variant.setIsActive(true);
                variant.setLockedStock(0);
                variant.setSkuCode(sku);

                if (varReq.getStockQuantity() != null) {
                    variant.setStockQuantity(varReq.getStockQuantity());
                } else {
                    variant.setStockQuantity(0);
                }

                // Image handling
                String rawImageUrl = varReq.getVariantImage();
                if (rawImageUrl != null && !rawImageUrl.isBlank()) {
                    rawImageUrl = rawImageUrl.replaceAll("^\"+|\"+$", "").trim();
                    String s3Key = rawImageUrl.startsWith("http")
                            ? rawImageUrl.replaceFirst(awsS3Properties.getBaseUrl() + "/?", "")
                            : rawImageUrl;
                    variant.setVariantImage(s3Key);
                    tempUploadRepository.deleteByUrl(rawImageUrl.startsWith("http")
                            ? rawImageUrl
                            : awsS3Properties.getBaseUrl() + "/" + s3Key);
                }

                productVariantRepository.save(variant);
                product.getVariants().add(variant);
            }
        }

        // Soft-delete (deactivate) or hard-delete các variants không có trong request (chỉ áp dụng khi update)
        if (productIdForSku != null) {
            java.util.List<ProductVariantEntity> toRemove = new java.util.ArrayList<>();
            for (ProductVariantEntity existing : product.getVariants()) {
                if (!requestSkus.contains(existing.getSkuCode())) {
                    boolean isReferenced = orderDetailRepository.existsByVariant_VariantId(existing.getVariantId());
                    if (isReferenced) {
                        existing.setIsActive(false); // Soft delete vì đã có đơn hàng
                    } else {
                        toRemove.add(existing); // Hard delete vì chưa từng được mua
                    }
                }
            }
            product.getVariants().removeAll(toRemove);
        }
    }

    /**
     * Build và lưu images từ request list.
     */
    private void buildAndSaveImages(List<ProductImageRequest> imageRequests, ProductEntity product) {
        if (imageRequests == null || imageRequests.isEmpty()) return;

        for (ProductImageRequest imgReq : imageRequests) {
            ProductImageEntity image = productMapper.toImageEntity(imgReq);
            image.setProduct(product);
            productImageRepository.save(image);
            product.getImages().add(image);

            // Xóa bản ghi trung chuyển — ảnh đã được liên kết chính thức
            if (imgReq.getImageUrl() != null) {
                tempUploadRepository.deleteByUrl(imgReq.getImageUrl());
            }
        }
    }

    /**
     * Resolve category code để sinh SKU.
     * Fallback về "PRD" nếu không có category hoặc không tìm thấy.
     */
    private String resolveCategoryCode(Long categoryId) {
        if (categoryId == null) return "PRD";
        return categoryRepository.findById(categoryId)
                .map(cat -> {
                    // CategoryCode đã là slug (ví dụ: "ao-thun"), lấy 3 ký tự đầu viết hoa
                    String code = cat.getCategoryCode();
                    if (code != null && !code.isBlank()) {
                        return code.replace("-", "").substring(0, Math.min(3, code.replace("-", "").length())).toUpperCase();
                    }
                    return "PRD";
                })
                .orElse("PRD");
    }
}
