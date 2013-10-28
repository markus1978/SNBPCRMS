# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table action (
  id                        bigint not null,
  service                   integer,
  action_type               integer,
  direction                 integer,
  message                   varchar(255),
  scheduled_for             timestamp,
  executed_at               timestamp,
  executed                  boolean,
  target_id                 bigint,
  constraint ck_action_service check (service in (0)),
  constraint ck_action_action_type check (action_type in (0,1,2,3)),
  constraint ck_action_direction check (direction in (0,1,2)),
  constraint pk_action primary key (id))
;

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

create sequence action_seq;

create sequence twitter_user_seq;

alter table action add constraint fk_action_target_1 foreign key (target_id) references twitter_user (id) on delete restrict on update restrict;
create index ix_action_target_1 on action (target_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists action;

drop table if exists twitter_user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists action_seq;

drop sequence if exists twitter_user_seq;

