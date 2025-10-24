# 📖 Transparent Book & Art Viewer

Kho lưu trữ này chứa mã nguồn của **Transparent**, một ứng dụng desktop giúp người dùng **xem và quản lý sách điện tử và tranh ảnh** trên Windows.  
Mục tiêu của dự án là mang lại **giao diện đơn giản, sạch sẽ**, hỗ trợ mở **PDF, hình ảnh, văn bản** và tự động lưu lại tiến trình đọc cùng danh sách yêu thích.

---

## ✨ Tính năng chính

- **Đăng nhập bằng Username** – Chỉ cần nhập tên người dùng để nhận dạng, **không cần mật khẩu**.  
- **Mở file** – Mở và đọc các định dạng phổ biến như **PDF, JPG, PNG, TXT…**.  
  Khi đóng file, **trang đang đọc sẽ được lưu tự động** vào lịch sử.  
- **Phân loại nội dung** – Khi thêm file, có thể nhập **Thể loại** (manga, tiểu thuyết, tranh nghệ thuật, v.v.) và **Tags**.  
  Bảng thư viện hiển thị hai cột này và cho phép **lọc theo thể loại hoặc tags**.  
- **Tìm kiếm và lọc** – Tìm kiếm theo **tiêu đề**, lọc theo **thể loại hoặc tags** để nhanh chóng tìm nội dung mong muốn.  
- **Sắp xếp** – Cho phép **sắp xếp danh sách thư viện** theo **Tiêu đề, Ngày thêm, Kích thước hoặc Loại file**.  
- **Xem trước (Preview)** – Cột *Preview* hiển thị **ảnh thu nhỏ** đối với file ảnh; các định dạng khác hiển thị chỗ trống *(có thể mở rộng với PDFBox)*.  
- **Lịch sử đọc** – Ghi lại **trang cuối cùng** và **thời gian đọc**, giúp bạn **tiếp tục dễ dàng** ở lần mở sau.  
- **Yêu thích** – Cho phép **đánh dấu nội dung yêu thích** để truy cập nhanh hơn.  
- **Bộ sưu tập cá nhân (Collections)** – Tạo **playlist hoặc thư mục riêng** để nhóm các nội dung liên quan.  
- **Giao diện sáng/tối** – Có nút **chuyển Theme** để **đổi giữa chủ đề sáng và tối** ngay trong ứng dụng.  

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

- Khi đóng file, ứng dụng sẽ **tự động lưu tiến trình đọc** (trang cuối cùng và thời gian đọc).  
- Các tính năng như **Preview PDF** và **Dark/Light Theme** có thể được mở rộng trong các phiên bản tương lai.  
- Bạn có thể chỉnh sửa hoặc thêm **thể loại và tags** để cá nhân hóa thư viện của mình.  
