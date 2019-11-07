/*
 * Author: Dan DaCosta
 * Company: Rockwell Collins
 * License: Air Force Open Source Agreement Version 1.0
 *
 * This file has been modified to be used as a simplified example on the CASE project
 * 
 */
#include "WaypointManager.h"
#include <stdbool.h>

/* ASM: ws != null */
/* ASM: len > 0 */
/* ASM: forall i < len, ws[i] is valid memory. */
Waypoint * FindWaypoint(const Waypoint * ws,
                        const uint16_t len,
                        const int64_t id) {
  for(uint16_t i = 0 ; i < len; i++) {
    if(ws[i].number == id) {
     return ws + i;
    }
  }
  return NULL;
}

/* NB: Cycles in ws will be unrolled into win. */
/* ASM: id can be found in ws. */
/* ASM: All next ids in the ws waypoint list can be found in ws. */
/* ASM: ws != null */
/* ASM: len_ws > 0. */
/* ASM: ws_win != null */
/* ASM: len_ws_win > 0 */
/* ASM: len_ws_win is less than the number of waypoints that can be
   stored in ws_win. */
/* ASM: Last waypoint starts a cycle. */
bool FillWindow(  Waypoint * ws
                  , uint16_t len_ws
                  , int64_t id
                  , uint16_t len_ws_win
                  , Waypoint * ws_win /* out */) {
  uint16_t i;
  int64_t nid = id;
  Waypoint * wp = NULL;
  bool success = true;
  for(i=0; i < len_ws_win && success == true; i++) {
    success = false;
    wp = FindWaypoint(ws, len_ws, nid);
    if(wp != NULL) {
      success = true;
      ws_win[i] = *wp;
      nid = ws_win[i].nextwaypoint;
    }
  }
  return success;
}

void GroomWindow(uint16_t len_ws_win
                , Waypoint * ws_win /* out */) {
  ws_win[len_ws_win-1].nextwaypoint = ws_win[len_ws_win-1].number;
  return;
}

/* NB: Cycles in ws will be unrolled into win. */
/* ASM: id can be found in ws. */
/* ASM: All next ids in the ws waypoint list can be found in ws. */
/* ASM: ws != null */
/* ASM: len_ws > 0. */
/* ASM: ws_win != null */
/* ASM: len_ws_win > 0 */
/* ASM: len_ws_win is less than the number of waypoints that can be
   stored in ws_win. */
/* ASM: Last waypoint starts a cycle. */
bool AutoPilotMissionCommandSegment(  Waypoint * ws
                                      , uint16_t len_ws
                                      , int64_t id
                                      , Waypoint * ws_win /* out */
                                      , uint16_t len_ws_win) {
  bool success = false;
  success = FillWindow(ws, len_ws, id, len_ws_win, ws_win);
  if(success == true) {GroomWindow(len_ws_win, ws_win);}
  return success;
}
