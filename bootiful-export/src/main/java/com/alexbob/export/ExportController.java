package com.alexbob.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * com.alexbob.export.ExportController
 *
 * @author Alex bob(https://github.com/vnobo)
 * @date Created by 2021/3/16
 */
@Controller
@RequestMapping("export")
@RequiredArgsConstructor
public class ExportController {

  private final Configuration freeMarkerConfig;
  private final ObjectMapper objectMapper;

  @GetMapping("doc")
  @ResponseBody
  public ResponseEntity<Resource> printDoc(Map<String, Object> params) {
    try {
      params = this.objectMapper.readValue("{\n"
          + "    \"year\":\"2021\",\n"
          + "    \"city\":\"西安\",\n"
          + "    \"jie\":\"ssss\",\n"
          + "    \"time\":\"8:00\",\n"
          + "    \"sequence\":[],\n"
          + "    \"allMeetingCount\":30,\n"
          + "    \"committeeMeetingCount\":40,\n"
          + "    \"delegationMeetingCount\":60,\n"
          + "    \"academyMeetingCount\":80,\n"
          + "    \"zhurenMeeting\":\"这是神马的\",\n"
          + "    \"xiaozuMeeting\":\"水水水水\",\n"
          + "    \"otherMeeting\":\"sss\",\n"
          + "    \"totalMeeting\":\"sssss\",\n"
          + "    \"specializedMeetingCount\":2222\n"
          + "}",Map.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    Resource file = loadAsResource(params);
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"我的统计.doc\"").body(file);
  }

  private Resource loadAsResource(Map<String,Object> params){
    try {
      Template template=this.freeMarkerConfig.getTemplate("mettingSta.ftl");
      String body= FreeMarkerTemplateUtils.processTemplateIntoString(template,params);
      return new ByteArrayResource(body.getBytes(StandardCharsets.UTF_8));
    } catch (IOException | TemplateException e) {
      e.printStackTrace();
    }
    return null;
  }
}