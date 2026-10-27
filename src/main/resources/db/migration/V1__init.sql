-- Skema awal QuickBite / ordering-system.
-- Dikelola Flyway; Hibernate hanya memvalidasi (ddl-auto: validate).

CREATE TABLE pelanggan (
    id_pelanggan  VARCHAR(36)  NOT NULL PRIMARY KEY,
    nama          VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    nomor_telepon VARCHAR(20),
    alamat        TEXT
);

CREATE TABLE menu (
    id_menu   VARCHAR(36)      NOT NULL PRIMARY KEY,
    nama_menu VARCHAR(100)     NOT NULL,
    harga     DOUBLE PRECISION NOT NULL,
    stok      INTEGER          NOT NULL
);

CREATE TABLE pesanan (
    id_pesanan   VARCHAR(36)      NOT NULL PRIMARY KEY,
    tanggal      TIMESTAMP(6)     NOT NULL,
    total_biaya  DOUBLE PRECISION NOT NULL,
    status       VARCHAR(20)      NOT NULL
        CHECK (status IN ('MENUNGGU_BAYAR', 'DIPROSES', 'DIKIRIM', 'SELESAI', 'BATAL')),
    id_pelanggan VARCHAR(36)      NOT NULL REFERENCES pelanggan (id_pelanggan)
);

CREATE TABLE item_pesanan (
    id_item    VARCHAR(36)      NOT NULL PRIMARY KEY,
    jumlah     INTEGER          NOT NULL,
    subtotal   DOUBLE PRECISION NOT NULL,
    id_pesanan VARCHAR(36)      NOT NULL REFERENCES pesanan (id_pesanan),
    id_menu    VARCHAR(36)      NOT NULL REFERENCES menu (id_menu)
);

CREATE TABLE pembayaran (
    id_pembayaran  VARCHAR(36)      NOT NULL PRIMARY KEY,
    jumlah_bayar   DOUBLE PRECISION NOT NULL,
    metode         VARCHAR(50)      NOT NULL,
    status_bayar   VARCHAR(50)      NOT NULL,
    kode_referensi VARCHAR(100),
    id_pesanan     VARCHAR(36)      NOT NULL UNIQUE REFERENCES pesanan (id_pesanan)
);
