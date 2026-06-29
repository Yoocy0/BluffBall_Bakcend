package com.project.bluffball.gametest.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 게임 로직 수동 테스트용 임시 화면 라우팅.
 *
 * <p>정적 파일은 {@code src/main/resources/static/game-test/} 에 둔다.</p>
 */
@Controller
@RequestMapping("/game-test")
public class GameTestPageController {

    @GetMapping({"", "/"})
    public String index() {
        return "redirect:/game-test/SetupNumber.html";
    }

    @GetMapping("/setup-number")
    public String setupNumber() {
        return "redirect:/game-test/SetupNumber.html";
    }

    @GetMapping("/mulligan")
    public String mulligan() {
        return "redirect:/game-test/Mulligan.html";
    }

    @GetMapping("/pitcher-select")
    public String pitcherSelect() {
        return "redirect:/game-test/PitcherSelect.html";
    }

    @GetMapping("/pitcher-result")
    public String pitcherResult() {
        return "redirect:/game-test/PitcherResult.html";
    }

    @GetMapping("/batter-select")
    public String batterSelect() {
        return "redirect:/game-test/BatterSelect.html";
    }

    @GetMapping("/batter-result")
    public String batterResult() {
        return "redirect:/game-test/BatterResult.html";
    }

    @GetMapping("/game-end")
    public String gameEnd() {
        return "redirect:/game-test/GameEnd.html";
    }
}
