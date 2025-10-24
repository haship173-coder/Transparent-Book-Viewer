# 📖 Transparent Book & Art Viewer

Kho lưu trữ này chứa mã nguồn của **Transparent**, một ứng dụng desktop giúp người dùng **xem và quản lý sách điện tử và tranh ảnh** trên Windows.  
Mục tiêu của dự án là mang lại **giao diện đơn giản, sạch sẽ**, hỗ trợ mở **PDF, hình ảnh, văn bản** và tự động lưu lại tiến trình đọc cùng danh sách yêu thích.

---

## ✨ Tính năng chính

- **Đăng nhập nhanh bằng username** – Không cần mật khẩu, tiện cho ứng dụng cá nhân/offline.
- **Thư viện offline-first** – Nếu không kết nối được tới SQL Server, dữ liệu sẽ tự động lưu vào thư mục `~/.transparent` và được đồng bộ lại khi chạy lần sau.
- **Nhập metadata khi thêm sách/tranh** – Sau khi chọn file, người dùng có thể điền *tiêu đề hiển thị*, *thể loại* và *tags* để quản lý khoa học hơn.
- **Bộ lọc mạnh mẽ** – Tìm kiếm theo tiêu đề, lọc theo tags và thể loại, hiển thị dấu sao cho nội dung yêu thích.
- **Trình đọc tích hợp** – Hiển thị hình ảnh, văn bản, EPUB (trích xuất chương đầu) trực tiếp trong app; PDF hoặc định dạng khác có thể mở bằng ứng dụng ngoài chỉ với một cú click.
- **Tự động lưu lịch sử & yêu thích** – Đánh dấu yêu thích từ thư viện hoặc ngay trong trình đọc; lịch sử đọc lưu thời gian và trang cuối cùng.
- **Màn hình lịch sử & yêu thích** – Double click để mở lại nội dung ngay từ các màn hình phụ.
- **Chủ đề sáng/tối** – Nút chuyển theme ngay trên thanh công cụ.

---

## 🧩 Công nghệ sử dụng

- **Java 17**, **JavaFX 17** – Xây dựng giao diện desktop
- **SQL Server Express** – Kho dữ liệu chính (các lớp DAO vẫn giữ nguyên)
- **Kho offline file-based** – Tự động ghi dữ liệu bằng cơ chế serialization khi không truy cập được DB
- **JavaFX ImageView/TextArea** – Hiển thị hình ảnh và văn bản
- **`java.awt.Desktop`** – Mở các định dạng đặc biệt (PDF, CBZ, …) bằng ứng dụng ngoài

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

- Dữ liệu offline nằm trong `~/.transparent/library.dat`. Có thể sao lưu/di chuyển để dùng trên máy khác.
- Nếu bạn bật lại SQL Server, ứng dụng vẫn hoạt động nhờ lớp DAO cũ – chỉ cần cập nhật thông tin kết nối trong `DBConnectionManager`.
- Các file PDF hiện được mở bằng ứng dụng mặc định trên Windows; bạn có thể tích hợp thêm PDFBox hoặc WebView để hiển thị trực tiếp.
