# 📖 Transparent Book & Art Viewer

Kho lưu trữ này chứa mã nguồn của **Transparent**, một ứng dụng desktop giúp người dùng **xem và quản lý sách điện tử và tranh ảnh** trên Windows.  
Mục tiêu của dự án là mang lại **giao diện đơn giản, sạch sẽ**, hỗ trợ mở **PDF, hình ảnh, văn bản** và tự động lưu lại tiến trình đọc cùng danh sách yêu thích.

---

## ✨ Tính năng chính

- **Đăng nhập siêu nhanh** – Chỉ cần nhập username, không cần mật khẩu hay đăng ký.
- **Hoạt động hoàn toàn offline** – Khi SQL Server không sẵn sàng, dữ liệu tự động chuyển sang **kho lưu trữ file nội bộ**. Thư viện, lịch sử, yêu thích và người dùng đều được đọc/ghi mượt mà.
- **Trình quản lý thư viện giàu thông tin** – Hiển thị tiêu đề, loại file, dung lượng, ngày thêm, thể loại và tags. Có thể **tìm kiếm**, **lọc theo tags hoặc thể loại**, và chỉnh sửa metadata ngay trong ứng dụng.
- **Hộp thoại metadata** – Khi thêm nội dung mới hoặc cập nhật file, người dùng có thể bổ sung tác giả, mô tả, tags… trước khi lưu.
- **Đánh dấu yêu thích & lịch sử đọc** – Bất cứ khi nào mở nội dung, ứng dụng ghi nhớ trang cuối cùng và thời điểm truy cập. Có sẵn danh sách lịch sử và yêu thích với thông tin chi tiết.
- **Chuyển theme sáng/tối tức thời** – Thanh công cụ chứa lựa chọn theme áp dụng cho toàn bộ giao diện và cả trình đọc WebView.
- **Trình đọc đa định dạng** – Hỗ trợ EPUB, PDF, hình ảnh và văn bản thuần. Với định dạng không hỗ trợ, người dùng có thể mở ngay bằng ứng dụng ngoài.
- **Ghi nhớ tiến độ** – Sau khi đóng trình đọc, trang cuối cùng được lưu để lần sau tiếp tục liền mạch.

---

## 🧩 Công nghệ sử dụng

- **Java 17**  
- **JavaFX 17** – Giao diện người dùng  
- **SQL Server Express** – Lưu trữ dữ liệu người dùng, nội dung, lịch sử và yêu thích  
- **Apache PDFBox** – Xử lý và hiển thị file PDF  
- **JavaFX ImageView** – Hiển thị hình ảnh (JPG/PNG)

---

## 🗂 Cấu trúc dự án

```
Transparent/
├── README.md
├── pom.xml
└── src
    └── main
        ├── java
        │   └── transparent
        │       ├── MainApp.java
        │       ├── db
        │       │   └── DBConnectionManager.java
        │       ├── model
        │       │   ├── User.java
        │       │   ├── Content.java
        │       │   ├── HistoryRecord.java
        │       │   └── Favourite.java
        │       ├── dao
        │       │   ├── UserDAO.java
        │       │   ├── ContentDAO.java
        │       │   ├── HistoryDAO.java
        │       │   └── FavouriteDAO.java
        │       ├── service
        │       │   ├── UserService.java
        │       │   ├── ContentService.java
        │       │   ├── HistoryService.java
        │       │   └── FavouriteService.java
        │       └── controller
        │           ├── CurrentUser.java
        │           ├── LoginController.java
        │           ├── MainController.java
        │           ├── ReaderController.java
        │           ├── HistoryController.java
        │           └── FavouritesController.java
        └── resources
            ├── login.fxml
            ├── main.fxml
            ├── reader.fxml
            ├── history.fxml
            └── favourites.fxml
```

---

## 💡 Ghi chú

- Ứng dụng luôn ưu tiên kết nối SQL Server; nếu kết nối thất bại, **toàn bộ dữ liệu sẽ được lấy từ kho file `.transparent/library-store.bin`** trong thư mục người dùng.
- Bộ lọc tìm kiếm hỗ trợ nhập nhiều tag (phân tách bởi dấu phẩy) và tùy chọn thể loại, giúp thu hẹp kết quả tức thì.
- Khi đóng trình đọc hoặc mở file bằng ứng dụng ngoài, tiến độ đọc được lưu lại để lần sau tiếp tục.
- Có thể chỉnh sửa metadata bất kỳ lúc nào bằng nút **Edit metadata** trong thư viện.
