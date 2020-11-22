package webserver.controller;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import webserver.graph.PlayerHeroGraph;
import webserver.util.ResultMsg;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GraphController {

    @Autowired
    private PlayerHeroGraph playerHeroGraph;

    @GetMapping("/get_graph")
    public ResultMsg getGraph(@RequestParam String graph,
                              @RequestParam(required = false) String players,
                              @RequestParam(required = false) String heroes,
                              @RequestParam(required = false) Integer lowerWins,
                              @RequestParam(required = false) Integer lowerMatches)
    {
        Map<String, Object> conditions = new HashMap<>();
        if (players != null) conditions.put("players", players);
        if (heroes != null) conditions.put("heroes", heroes);
        if (lowerWins != null) conditions.put("lowerWins", lowerWins);
        if (lowerMatches != null) conditions.put("lowerMatches", lowerMatches);

        if (graph.equals("player_hero")) {
            JSONObject graphinfo = playerHeroGraph.toJSON(playerHeroGraph.getGraph(conditions));
            return new ResultMsg(ResultMsg.SUCCESS, "", graphinfo);
        }
        return new ResultMsg(ResultMsg.FAILURE, "graph not exists");
    }
}
