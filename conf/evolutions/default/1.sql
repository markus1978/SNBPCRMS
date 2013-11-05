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
  constraint ck_action_service check (service in (0,1,2)),
  constraint ck_action_action_type check (action_type in (0,1,2,3,4)),
  constraint ck_action_direction check (direction in (0,1,2)),
  constraint pk_action primary key (id))
;

create table presence (
  id                        bigint not null,
  tier                      integer,
  category                  integer,
  i_os                      boolean,
  android                   boolean,
  pc                        boolean,
  consoles                  boolean,
  more                      boolean,
  name                      varchar(255),
  channel_urls_blob         varchar(255),
  contact_urls_blob         varchar(255),
  last_activity             timestamp,
  added                     timestamp,
  constraint ck_presence_tier check (tier in (0,1,2,3)),
  constraint ck_presence_category check (category in (0,1,2,3)),
  constraint pk_presence primary key (id))
;

create table twitter_user (
  id                        bigint not null,
  screen_name               varchar(255),
  description               varchar(255),
  name                      varchar(255),
  image_url                 varchar(255),
  followers_count           integer,
  friends_count             integer,
  tweet_count               integer,
  added                     timestamp,
  last_updated              timestamp,
  is_follower               boolean,
  is_follower_since         timestamp,
  times_has_been_follower   integer,
  is_friend                 boolean,
  is_friend_since           timestamp,
  times_has_been_friend     integer,
  is_starred                boolean,
  presence_id               bigint,
  constraint pk_twitter_user primary key (id))
;

create sequence action_seq;

create sequence presence_seq;

create sequence twitter_user_seq;

alter table action add constraint fk_action_target_1 foreign key (target_id) references presence (id) on delete restrict on update restrict;
create index ix_action_target_1 on action (target_id);
alter table twitter_user add constraint fk_twitter_user_presence_2 foreign key (presence_id) references presence (id) on delete restrict on update restrict;
create index ix_twitter_user_presence_2 on twitter_user (presence_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists action;

drop table if exists presence;

drop table if exists twitter_user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists action_seq;

drop sequence if exists presence_seq;

drop sequence if exists twitter_user_seq;

