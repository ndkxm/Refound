# Bộ mẫu đối chiếu (P0-13)

Dùng cho hai việc: đo độ chính xác (`./mvnw test -Dgroups=eval`, 04 §4.6) và nạp dữ liệu demo (`--seed`, 09 §9.3).

## Cần chuẩn bị

- 30 tin `found` có ảnh **do nhóm tự chụp trong trường** (không lấy ảnh mạng), 30 tin `lost`, mỗi tin lost chỉ ra tin found đúng.
- "Bẫy": 3 tai nghe đen, 3 bình nước xanh, 2 ví nâu, 2 thẻ sinh viên **giả** (không dùng thẻ thật).
- Một nửa tin `lost` chỉ có chữ, viết kiểu sinh viên thật ("tai nghe trắng hộp sứt góc").
- Ảnh JPEG trong `images/`, **đã xoay đúng chiều** (code không đọc EXIF), cạnh dài bao nhiêu cũng được: code tự thu về 1024 và 512 px.

## `dataset.json`

```json
{
  "items": [
    {
      "id": "f01",
      "type": "found",
      "category": "earphones",
      "description": "Tai nghe AirPods hộp trắng, nắp có vết xước",
      "happenedAt": "2026-11-20T09:30:00+07:00",
      "lat": 10.8701,
      "lng": 106.8031,
      "images": ["f01a.jpg", "f01b.jpg"],
      "question": "Hộp tai nghe có dán gì?",
      "answer": "Sticker con mèo"
    },
    {
      "id": "l01",
      "type": "lost",
      "category": "earphones",
      "description": "mất tai nghe trắng hộp sứt góc ở thư viện tầng 2",
      "happenedAt": "2026-11-20T08:00:00+07:00",
      "lat": 10.8705,
      "lng": 106.8028,
      "radiusM": 200,
      "images": [],
      "expectedFoundId": "f01"
    }
  ]
}
```

- `category`: một trong 14 mã ở migration P0 (`student_card`, `id_card`, `bank_card`, `wallet`, `bag`, `keys`, `phone`, `earphones`, `laptop`, `charger`, `clothing`, `bottle`, `book`, `other`).
- `description` 5–1000 ký tự; tối đa 4 ảnh; tin found phải có ảnh; `radiusM` 50–2000 (mặc định 200).
- Danh mục nhạy cảm (3 loại thẻ): ảnh không gửi AI (BR-08); tin lost nhạy cảm nạp demo không kèm ảnh.
- Gõ sai tên trường thì test báo lỗi ngay.

## Chạy

```bash
# Lần đầu: tính vector (cần key, khoảng 60 ảnh + 120 đoạn chữ, gửi theo lô 50) rồi lưu cache-gemini-embedding-2.json
GEMINI_API_KEY=… ./mvnw test -Dgroups=eval
# Các lần sau: đọc cache, không tốn request. Đổi ảnh/mô tả thì chỉ phần đổi được tính lại.
./mvnw test -Dgroups=eval

# Nạp 60 tin làm dữ liệu demo (tin có đủ vector trong cache thì gắn luôn, còn lại job embedding làm)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--seed
```

Kết quả in bảng top-1, top-5, MRR cho 4 nhóm (chữ↔chữ, chữ↔ảnh, ảnh↔ảnh, kết hợp như `/search`), phân bố cosine
của cặp đúng / sai (để chọn `c0`, `c1` ở H01) và p50 thời gian một lời gọi Gemini. Commit cả `cache-*.json`.
Khóa vector ảnh là sha256 của ảnh đã thu nhỏ: JDK khác có thể nén ra byte khác, khi đó ảnh được tính lại (hoặc `--seed` để tin `pending` cho job).

Chưa có: mô tả ảnh của Groq (`ai_caption`) không đưa vào chữ khi đo, nên số đo là cận dưới so với server thật.
