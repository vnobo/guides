package com.alexbob.web;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * com.alexbob.web.SyncService
 *
 * @author Alex bob(https://github.com/vnobo)
 * @date Created by 2020/7/6
 */
@Log4j2
@Service
public class SyncService {

    private int x = 0;

    public void log() {
        log.info("Sync log x {}", x);
    }

    public void setX(int x) {
        this.x = x;
    }
}
