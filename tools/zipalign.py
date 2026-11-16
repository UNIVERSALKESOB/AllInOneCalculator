#!/usr/bin/env python3
"""Minimal zipalign replacement: keeps stored entries 4-byte aligned."""
import struct
import sys

ALIGN = 4
ALIGN_EXTRA_ID = 0xD935


def main(src, dst):
    raw = open(src, "rb").read()
    eocd = raw.rfind(b"PK\x05\x06")
    (_, disk, cd_disk, n_disk, n_total, cd_size, cd_off,
     comment_len) = struct.unpack("<IHHHHIIH", raw[eocd:eocd + 22])

    entries = []
    pos = cd_off
    for _ in range(n_total):
        (sig, ver_made, ver_need, flags, method, mtime, mdate, crc,
         csize, usize, name_len, extra_len, clen, disk_start, iattr,
         eattr, lho) = struct.unpack("<IHHHHHHIIIHHHHHII", raw[pos:pos + 46])
        name = raw[pos + 46:pos + 46 + name_len]
        extra = raw[pos + 46 + name_len:pos + 46 + name_len + extra_len]
        comment2 = raw[pos + 46 + name_len + extra_len:pos + 46 + name_len + extra_len + clen]
        entries.append(dict(ver_made=ver_made, ver_need=ver_need, flags=flags,
                            method=method, mtime=mtime, mdate=mdate, crc=crc,
                            csize=csize, usize=usize, name=name, extra=extra,
                            comment=comment2, iattr=iattr, eattr=eattr, lho=lho))
        pos += 46 + name_len + extra_len + clen

    out = bytearray()
    central = []

    for e in entries:
        lho = e["lho"]
        lnlen, lextra = struct.unpack("<HH", raw[lho + 26:lho + 30])
        data_start = lho + 30 + lnlen + lextra
        data = raw[data_start:data_start + e["csize"]]

        new_off = len(out)
        extra = e["extra"]
        if e["method"] == 0:  # stored entry -> align payload
            header_end = new_off + 30 + len(e["name"]) + len(extra)
            pad = (ALIGN - header_end % ALIGN) % ALIGN
            if pad:
                if pad < 4:
                    pad += ALIGN
                extra = extra + struct.pack("<HH", ALIGN_EXTRA_ID, pad - 4) + b"\x00" * (pad - 4)

        out += struct.pack("<IHHHHHIIIHH", 0x04034b50, e["ver_need"], e["flags"],
                           e["method"], e["mtime"], e["mdate"], e["crc"],
                           e["csize"], e["usize"], len(e["name"]), len(extra))
        out += e["name"] + extra + data

        central.append((e, new_off, extra, len(extra)))

    cd_start = len(out)
    for e, new_off, extra, extra_len in central:
        out += struct.pack("<IHHHHHHIIIHHHHHII", 0x02014b50, e["ver_made"],
                           e["ver_need"], e["flags"], e["method"], e["mtime"],
                           e["mdate"], e["crc"], e["csize"], e["usize"],
                           len(e["name"]), extra_len, len(e["comment"]), 0,
                           e["iattr"], e["eattr"], new_off)
        out += e["name"] + extra + e["comment"]

    cd_size = len(out) - cd_start
    out += struct.pack("<IHHHHIIH", 0x06054b50, 0, 0, n_disk, n_total,
                       cd_size, cd_start, comment_len)
    out += raw[eocd + 22:eocd + 22 + comment_len]

    with open(dst, "wb") as f:
        f.write(out)
    print("aligned", dst, len(out), "bytes")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
