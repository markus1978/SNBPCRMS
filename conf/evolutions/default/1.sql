# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table twitter_user (
  id                        bigint not null,
  screen_name               varchar(255),
  added                     timestamp,
  last_updated              timestamp,
  is_follower               boolean,
  is_follower_since         timestamp,
  times_has_been_follower   integer,
  is_friend                 boolean,
  is_friend_since           timestamp,
  times_has_been_friend     integer,
  followers_count           integer,
  friends_count             integer,
  tier                      integer,
  category                  integer,
  status                    integer,
  description               varchar(255),
  constraint ck_twitter_user_tier check (tier in (0,1,2,3)),
  constraint ck_twitter_user_category check (category in (0,1,2,3)),
  constraint ck_twitter_user_status check (status in (0,1,2,3)),
  constraint pk_twitter_user primary key (id))
;

create sequence twitter_user_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists twitter_user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists twitter_user_seq;

