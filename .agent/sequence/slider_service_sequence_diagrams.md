# Sequence Diagrams for Slider Service

Tài liệu này chứa các sơ đồ tuần tự cho các hoạt động trong `SliderServiceImpl`.

## 1. Tạo Slider (`createSlider`)

```mermaid
sequenceDiagram
    participant Client
    participant SliderService
    participant SliderMapper
    participant SliderRepository

    Client->>SliderService: createSlider(SliderCreateRequest)
    activate SliderService

    SliderService->>SliderMapper: toEntity(request)
    SliderMapper-->>SliderService: SliderEntity

    SliderService->>SliderRepository: save(slider)
    activate SliderRepository
    SliderRepository-->>SliderService: savedSlider
    deactivate SliderRepository

    SliderService->>SliderMapper: toResponse(savedSlider)
    SliderMapper-->>SliderService: SliderResponse
    
    SliderService-->>Client: SliderResponse
    deactivate SliderService
```

## 2. Cập nhật Slider (`updateSlider`)

```mermaid
sequenceDiagram
    participant Client
    participant SliderService
    participant SliderRepository
    participant SliderMapper

    Client->>SliderService: updateSlider(sliderId, SliderUpdateRequest)
    activate SliderService

    SliderService->>SliderRepository: findById(sliderId)
    activate SliderRepository
    SliderRepository-->>SliderService: SliderEntity (hoặc ném ra NOT_FOUND)
    deactivate SliderRepository

    SliderService->>SliderMapper: updateEntityFromRequest()

    SliderService->>SliderRepository: save(slider)
    activate SliderRepository
    SliderRepository-->>SliderService: savedSlider
    deactivate SliderRepository

    SliderService->>SliderMapper: toResponse(savedSlider)
    SliderMapper-->>SliderService: SliderResponse
    
    SliderService-->>Client: SliderResponse
    deactivate SliderService
```

## 3. Xóa Slider (`deleteSlider`)

```mermaid
sequenceDiagram
    participant Client
    participant SliderService
    participant SliderRepository

    Client->>SliderService: deleteSlider(sliderId)
    activate SliderService

    SliderService->>SliderRepository: findById(sliderId)
    activate SliderRepository
    SliderRepository-->>SliderService: SliderEntity (hoặc ném ra NOT_FOUND)
    deactivate SliderRepository

    SliderService->>SliderRepository: delete(slider)
    activate SliderRepository
    SliderRepository-->>SliderService: void
    deactivate SliderRepository

    SliderService-->>Client: void
    deactivate SliderService
```

## 4. Lấy tất cả Sliders (`getAllSliders`) - Dành cho Admin

```mermaid
sequenceDiagram
    participant Client
    participant SliderService
    participant SliderRepository
    participant SliderMapper

    Client->>SliderService: getAllSliders(pageable)
    activate SliderService

    SliderService->>SliderRepository: findAll(pageable)
    activate SliderRepository
    SliderRepository-->>SliderService: Page<SliderEntity>
    deactivate SliderRepository

    SliderService->>SliderMapper: toResponse() (cho mỗi slider)
    
    SliderService-->>Client: Page<SliderResponse>
    deactivate SliderService
```

## 5. Lấy các Sliders đang hoạt động (`getActiveSliders`) - Dành cho Public UI

```mermaid
sequenceDiagram
    participant Client
    participant SliderService
    participant SliderRepository
    participant SliderMapper

    Client->>SliderService: getActiveSliders()
    activate SliderService

    SliderService->>SliderRepository: findByIsActiveTrueOrderByDisplayOrderAsc()
    activate SliderRepository
    SliderRepository-->>SliderService: List<SliderEntity>
    deactivate SliderRepository

    SliderService->>SliderMapper: toResponseList(sliders)
    SliderMapper-->>SliderService: List<SliderResponse>
    
    SliderService-->>Client: List<SliderResponse>
    deactivate SliderService
```
