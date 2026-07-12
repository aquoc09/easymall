# Session Report: Product Filtering & Pagination Implementation

## 1. Context & Objective
The existing Product Listing API (`GET /api/v1/products/public` and `GET /api/v1/products`) was returning a flat list of all products without any pagination or filtering mechanisms. The objective of this session was to implement a robust, scalable, and dynamic filtering system to support e-commerce requirements such as:
- Viewing products by specific collections (`NEW_ARRIVALS`, `BEST_SELLERS`, `POPULAR`).
- Filtering products by categories (including all child categories).
- Filtering by attributes like `minPrice`, `maxPrice`, `targetGender`, and `minRating`.
- Keyword searching across product names and slugs.
- Returning paginated data structures.

## 2. Technical Approach
To avoid writing numerous native queries and complex `@Query` annotations, we adopted **Spring Data JPA Specifications**. This approach allows us to dynamically construct `WHERE` clauses based on the fields present in the incoming request.

## 3. Key Components Implemented

### A. DTO Layer (`ProductFilterRequest.java`)
Created a dedicated DTO to capture all possible query parameters from the client.
- **Fields:** `keyword`, `categoryCode`, `collection`, `minPrice`, `maxPrice`, `minRating`, `targetGender`, `inStock`, `inPopular`.
- Bound via `@ModelAttribute` in the controller to automatically parse query strings.

### B. Repository Layer (`CategoryRepository.java`)
- Added `findByCategoryCode` to resolve the category ID from the code.
- Added `findByParentId` to fetch all sub-categories. This ensures that filtering by a parent category (e.g., "Men's Clothing") automatically includes products in child categories (e.g., "Men's Jackets", "Men's Shirts").

### C. Specification Layer (`ProductSpecification.java`)
Implemented a factory class to generate a `Specification<ProductEntity>`.
- Uses `CriteriaBuilder` to append `Predicate`s dynamically.
- Handles custom `Collection` logic:
  - `POPULAR`: Adds a constraint `inPopular = true`.
  - `NEW_ARRIVALS` and `BEST_SELLERS` do not filter rows, but rely on sorting logic in the service layer.

### D. Service Layer (`ProductServiceImpl.java`)
- Refactored `getAllProducts` (Admin) and `getPublicProducts` (User).
- **Public API logic:** Forcefully sets `inStock = true` so users never see disabled products.
- **Sorting logic:** Dynamically alters the `Pageable` object if a `collection` is specified. For example, `NEW_ARRIVALS` forces sorting by `createdAt DESC`, overriding client sorts.
- Mapped `ProductEntity` streams to `ProductResponse` streams efficiently using the existing `ProductMapper`.

### E. Controller Layer (`ProductController.java`)
- Modified endpoint signatures to accept `ProductFilterRequest` and `Pageable` parameters.
- Standardized the return type to `ApiResponse<Page<ProductResponse>>`.

### F. Postman Integration
- Connected to the `postman-mcp-server` to automatically create a new request named **Get All Products (Filtered)** inside the EasyMall workspace (`[Public] Products` folder).
- Pre-filled query parameters to demonstrate typical usage to the Frontend team.

## 4. Challenges & Resolutions
- **Category Hierarchy:** Products are assigned to leaf categories. To filter by a parent category, we had to recursively (or single-level) find all child category IDs and use a SQL `IN` clause. 
  - *Resolution:* Queried the `CategoryRepository` for children before building the `Specification`, then used `root.get("category").get("id").in(categoryIds)`.
- **API Contract Breaking Change:** The endpoint now returns a `Page` object (`content`, `totalElements`, `totalPages`) instead of a raw `List`.
  - *Resolution:* Highlighted this change in the walkthrough and explicitly created a Postman request for the Frontend team to test and adapt.

## 5. Next Steps / Pending Items
- **Contact Message Refactoring:** In the upcoming sessions, we need to address the `ContactMessageRequest` validation to only require `guestName` and `guestEmail` when the user is not authenticated.
- **Product Variant Refactoring (Pending Analysis):** We have a pending architectural discussion regarding the `updateProduct` flow, specifically around handling `SKU` changes and soft-deleting old variants versus updating them.
