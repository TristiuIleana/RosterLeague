/*
 * Copyright (c), Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v1.0, which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.rosterleague.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.rosterleague.common.*;
import org.rosterleague.entities.*;

/**
 * This is the bean class for the RequestBean enterprise bean.
 *
 * @author ian
 */
@Stateless
public class RequestBean implements Request, Serializable {

    private static final Logger logger = Logger.getLogger(RequestBean.class.getName());

    @PersistenceContext
    private EntityManager em;

    private CriteriaBuilder cb;

    @PostConstruct
    private void init() {
        cb = em.getCriteriaBuilder();
    }

    @Override
    public void createPlayer(String id, String name, String position, double salary) {
        logger.info("createPlayer");
        try {
            Player player = new Player(id, name, position, salary);
            em.persist(player);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public void addPlayer(String playerId, String teamId) {
        logger.info("addPlayer");
        try {
            Player player = em.find(Player.class, playerId);
            Team team = em.find(Team.class, teamId);

            team.addPlayer(player);
            player.addTeam(team);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public void removePlayer(String playerId) {
        logger.info("removePlayer");
        try {
            Player player = em.find(Player.class, playerId);

            Collection<Team> teams = player.getTeams();
            for (Team team : teams) {
                team.dropPlayer(player);
            }

            em.remove(player);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public void dropPlayer(String playerId, String teamId) {
        logger.info("dropPlayer");
        try {
            Player player = em.find(Player.class, playerId);
            Team team = em.find(Team.class, teamId);

            team.dropPlayer(player);
            player.dropTeam(team);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public PlayerDetails getPlayer(String playerId) {
        logger.info("getPlayerDetails");
        try {
            Player player = em.find(Player.class, playerId);
            PlayerDetails playerDetails = new PlayerDetails(player.getId(), player.getName(), player.getPosition(), player.getSalary());
            return playerDetails;
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersOfTeam(String teamId) {
        logger.info("getPlayersOfTeam");
        List<PlayerDetails> playerList;
        try {
            Team team = em.find(Team.class, teamId);
            playerList = this.copyPlayersToDetails((List<Player>) team.getPlayers());
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return playerList;
    }

    @Override
    public List<TeamDetails> getTeamsOfLeague(String leagueId) {
        logger.info("getTeamsOfLeague");
        List<TeamDetails> detailsList = new ArrayList<>();
        Collection<Team> teams;

        try {
            League league = em.find(League.class, leagueId);
            teams = league.getTeams();
        } catch (Exception ex) {
            throw new EJBException(ex);
        }

        for (Team team : teams) {
            TeamDetails teamDetails = new TeamDetails(team.getId(), team.getName(), team.getCity());
            detailsList.add(teamDetails);
        }
        return detailsList;
    }

    @Override
    public List<PlayerDetails> getPlayersByPosition(String position) {
        logger.info("getPlayersByPosition");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);

                // set the where clause
                cq.where(cb.equal(player.get(Player_.position), position));
                cq.select(player);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersByHigherSalary(String name) {
        logger.info("getPlayersByHigherSalary");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player1 = cq.from(Player.class);
                Root<Player> player2 = cq.from(Player.class);

                // create a Predicate object that finds players with a salary
                // greater than player1
                Predicate gtPredicate = cb.greaterThan(player1.get(Player_.salary), player2.get(Player_.salary));
                // create a Predicate object that finds the player based on
                // the name parameter
                Predicate equalPredicate = cb.equal(player2.get(Player_.name), name);
                // set the where clause with the predicates
                cq.where(gtPredicate, equalPredicate);
                // set the select clause, and return only unique entries
                cq.select(player1).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersBySalaryRange(double low, double high) {
        logger.info("getPlayersBySalaryRange");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);

                // set the where clause
                cq.where(cb.between(player.get(Player_.salary), low, high));
                // set the select clause
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersByLeagueId(String leagueId) {
        logger.info("getPlayersByLeagueId");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);
                Join<Player, Team> team = player.join(Player_.teams);
                Join<Team, League> league = team.join(Team_.league);

                // set the where clause
                cq.where(cb.equal(league.get(League_.id), leagueId));
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersBySport(String sport) {
        logger.info("getPlayersByLeagueId");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);
                Join<Player, Team> team = player.join(Player_.teams);
                Join<Team, League> league = team.join(Team_.league);

                // set the where clause
                cq.where(cb.equal(league.get(League_.sport), sport));
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersByCity(String city) {
        logger.info("getPlayersByCity");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);
                Join<Player, Team> team = player.join(Player_.teams);

                // set the where clause
                cq.where(cb.equal(team.get(Team_.city), city));
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getAllPlayers() {
        logger.info("getAllPlayers");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);

                cq.select(player);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersNotOnTeam() {
        logger.info("getPlayersNotOnTeam");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);

                // set the where clause
                cq.where(cb.isEmpty(player.get(Player_.teams)));
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<PlayerDetails> getPlayersByPositionAndName(String position, String name) {
        logger.info("getPlayersByPositionAndName");
        List<Player> players = null;

        try {
            CriteriaQuery<Player> cq = cb.createQuery(Player.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);

                // set the where clause
                cq.where(cb.equal(player.get(Player_.position), position), cb.equal(player.get(Player_.name), name));
                cq.select(player).distinct(true);
                TypedQuery<Player> q = em.createQuery(cq);
                players = q.getResultList();
            }
            return copyPlayersToDetails(players);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public List<LeagueDetails> getLeaguesOfPlayer(String playerId) {
        logger.info("getLeaguesOfPlayer");
        List<LeagueDetails> detailsList = new ArrayList<>();
        List<League> leagues = null;

        try {
            CriteriaQuery<League> cq = cb.createQuery(League.class);
            if (cq != null) {
                Root<League> league = cq.from(League.class);
                Join<League, Team> team = league.join(League_.teams);
                Join<Team, Player> player = team.join(Team_.players);

                cq.where(cb.equal(player.get(Player_.id), playerId));
                cq.select(league).distinct(true);
                TypedQuery<League> q = em.createQuery(cq);
                leagues = q.getResultList();
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }

        if (leagues == null) {
            logger.log(Level.WARNING, "No leagues found for player with ID {0}.", playerId);
            return null;
        } else {
            for (League league : leagues) {
                LeagueDetails leagueDetails = new LeagueDetails(league.getId(), league.getName(), league.getSport());
                detailsList.add(leagueDetails);
            }

        }
        return detailsList;
    }

    @Override
    public List<String> getSportsOfPlayer(String playerId) {
        logger.info("getSportsOfPlayer");
        List<String> sports = new ArrayList<>();

        try {
            CriteriaQuery<String> cq = cb.createQuery(String.class);
            if (cq != null) {
                Root<Player> player = cq.from(Player.class);
                Join<Player, Team> team = player.join(Player_.teams);
                Join<Team, League> league = team.join(Team_.league);

                // set the where clause
                cq.where(cb.equal(player.get(Player_.id), playerId));
                cq.select(league.get(League_.sport)).distinct(true);
                TypedQuery<String> q = em.createQuery(cq);
                sports = q.getResultList();
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return sports;
    }

    @Override
    public void createTeamInLeague(TeamDetails teamDetails, String leagueId) {
        logger.info("createTeamInLeague");
        try {
            League league = em.find(League.class, leagueId);
            Team team = new Team(teamDetails.getId(), teamDetails.getName(), teamDetails.getCity());
            em.persist(team);
            team.setLeague(league);
            league.addTeam(team);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public void removeTeam(String teamId) {
        logger.info("removeTeam");
        try {
            Team team = em.find(Team.class, teamId);

            Collection<Player> players = team.getPlayers();
            for (Player player : players) {
                player.dropTeam(team);
            }

            em.remove(team);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public TeamDetails getTeam(String teamId) {
        logger.info("getTeam");
        TeamDetails teamDetails;

        try {
            Team team = em.find(Team.class, teamId);
            teamDetails = new TeamDetails(team.getId(), team.getName(), team.getCity());
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return teamDetails;
    }

    @Override
    public void createLeague(LeagueDetails leagueDetails) {
        logger.info("createLeague");
        try {
            if (leagueDetails.getSport().equalsIgnoreCase("soccer") || leagueDetails.getSport().equalsIgnoreCase("swimming") || leagueDetails.getSport().equalsIgnoreCase("basketball") || leagueDetails.getSport().equalsIgnoreCase("baseball")) {
                SummerLeague league = new SummerLeague(leagueDetails.getId(), leagueDetails.getName(), leagueDetails.getSport());
                em.persist(league);
            } else if (leagueDetails.getSport().equalsIgnoreCase("hockey") || leagueDetails.getSport().equalsIgnoreCase("skiing") || leagueDetails.getSport().equalsIgnoreCase("snowboarding")) {
                WinterLeague league = new WinterLeague(leagueDetails.getId(), leagueDetails.getName(), leagueDetails.getSport());
                em.persist(league);
            } else {
                throw new IncorrectSportException("The specified sport is not valid.");
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public void removeLeague(String leagueId) {
        logger.info("removeLeague");
        try {
            League league = em.find(League.class, leagueId);
            em.remove(league);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public LeagueDetails getLeague(String leagueId) {
        logger.info("getLeague");
        LeagueDetails leagueDetails;

        try {
            League league = em.find(League.class, leagueId);
            leagueDetails = new LeagueDetails(league.getId(), league.getName(), league.getSport());
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return leagueDetails;
    }
    @Override
    public void setupTournamentData() {
        try {
            createLeague(new LeagueDetails("CL", "Champions League", "Soccer"));
            createLeague(new LeagueDetails("USC", "UEFA Super Cup", "Soccer"));
            createLeague(new LeagueDetails("FIC", "FIFA International Cup", "Soccer"));

            String[] ids = {"T_SB", "T_HD", "T_BV", "T_CJ", "T_TM"};
            String[] names = {"Real Madrid", "Barcelona", "Manchester United", "Bayern Munchen", "Juventus"};
            String[] cities = {"Sibiu", "Hunedoara", "Brasov", "Cluj", "Timisoara"};

            for (int k = 0; k < ids.length; k++) {
                createTeamInLeague(new TeamDetails(ids[k], names[k], cities[k]), "CL");
            }

            int pIndex = 1;
            for (String teamId : ids) {
                for (int count = 0; count < 5; count++) {
                    String pId = "P" + pIndex;
                    double salary = ThreadLocalRandom.current().nextInt(1000, 10001);
                    createPlayer(pId, pId, "jucator", salary);
                    addPlayer(pId, teamId);
                    pIndex++;
                }
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public String playTournament(String leagueId, String nextLeagueId) {
        StringBuilder sb = new StringBuilder();

        try {
            League currentLeague = em.find(League.class, leagueId);
            List<Team> leagueTeams = new ArrayList<>(currentLeague.getTeams());

            leagueTeams.forEach(Team::resetPoints);

            sb.append("--- ").append(currentLeague.getName()).append(" : Mathes ---\n");

            for (int a = 0; a < leagueTeams.size() - 1; a++) {
                for (int b = a + 1; b < leagueTeams.size(); b++) {
                    Team home = leagueTeams.get(a);
                    Team away = leagueTeams.get(b);

                    int scoreHome = ThreadLocalRandom.current().nextInt(0, 6);
                    int scoreAway = ThreadLocalRandom.current().nextInt(0, 6);

                    assignMatchPoints(home, away, scoreHome, scoreAway);

                    sb.append(String.format("\"%s\":%d - \"%s\":%d - math:%d-%d\n",
                            home.getName(), home.getPoints(),
                            away.getName(), away.getPoints(),
                            scoreHome, scoreAway));
                }
            }

            leagueTeams.sort((t1, t2) -> Integer.compare(t2.getPoints(), t1.getPoints()));

            double[] prizes = {30000.0, 20000.0, 10000.0};
            for (int pos = 0; pos < Math.min(3, leagueTeams.size()); pos++) {
                double bonus = prizes[pos];
                for (Player p : leagueTeams.get(pos).getPlayers()) {
                    p.setSalary(p.getSalary() + bonus);
                }
            }

            sb.append("\n--- ").append(currentLeague.getName()).append(" : Clasament ---\n");
            for (int pos = 0; pos < leagueTeams.size(); pos++) {
                Team team = leagueTeams.get(pos);
                List<Player> roster = new ArrayList<>(team.getPlayers());
                roster.sort(Comparator.comparingInt(p -> Integer.parseInt(p.getName().substring(1))));

                for (Player p : roster) {
                    sb.append(String.format("\"%s\", place %d, \"%s\", player %s - %.2f\n",
                            currentLeague.getName(), pos + 1, team.getName(),
                            p.getName(), p.getSalary()));
                }
            }
            sb.append("\n");

            if (nextLeagueId != null && leagueTeams.size() > 1) {
                League nextLeague = em.find(League.class, nextLeagueId);
                Team lastTeam = leagueTeams.get(leagueTeams.size() - 1);
                currentLeague.dropTeam(lastTeam);
                lastTeam.setLeague(null);

                for (int i = 0; i < leagueTeams.size() - 1; i++) {
                    Team advancingTeam = leagueTeams.get(i);
                    currentLeague.dropTeam(advancingTeam);
                    advancingTeam.setLeague(nextLeague);
                    nextLeague.addTeam(advancingTeam);
                }
            }

            return sb.toString();
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    private void assignMatchPoints(Team home, Team away, int scoreHome, int scoreAway) {
        if (scoreHome > scoreAway) {
            home.addPoints(3);
        } else if (scoreAway > scoreHome) {
            away.addPoints(3);
        } else {
            home.addPoints(1);
            away.addPoints(1);
        }
    }
    @Override
    public void clearAllEntities() {
        em.createQuery("DELETE FROM Player").executeUpdate();
        em.createQuery("DELETE FROM Team").executeUpdate();
        em.createQuery("DELETE FROM League").executeUpdate();
    }

    private List<PlayerDetails> copyPlayersToDetails(List<Player> players) {
        List<PlayerDetails> detailsList = new ArrayList<>();
        for (Player player : players) {
            PlayerDetails playerDetails = new PlayerDetails(player.getId(), player.getName(), player.getPosition(), player.getSalary());
            detailsList.add(playerDetails);
        }
        return detailsList;
    }
}
