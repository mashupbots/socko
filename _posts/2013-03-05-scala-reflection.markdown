---
layout: post
title: REST handler and Scala Reflection
summary: What's been happening?
author: Vibul
---

Finally my day job is returning to a normal day job. Since November last year up until last week, it has been a day/night/weekend job.

I have started playing around with Scala 2.10 reflection and planning to use it in the Socko REST handler implementation.

I want to implement the REST handler in such a way that it supports [Swagger API](https://developers.helloreverb.com/swagger/) documentation.

I noticed that the way you document your Swagger data model object is via annotations.  For example:


    case class Pet(var id: Long = 0,
      var category: Category,
      var name: String,
      var photoUrls: List[String],
      var tags: List[Tag] = List[Tag](),
      @ApiProperty(value = "pet status in the store", allowableValues = "available,pending,sold") var status: String)


I am a stickler for consistency so I am thinking of using annotations for REST endpoint definitions too. Maybe 
something like:


    @RestGet(
      uriTemplate = "/pets",
      actorPath = "my/actor/path"
    )
    case class GetPetRequest()


Now, if we want to use annotations, then we will need to use reflection.

It has taken me a while to get up to speed with reflection and here are a couple of links illustrating
what I've learnt so far.

 - [Scala 2.10 Runtime Reflection from a Class Name](http://www.veebsbraindump.com/2013/03/scala-2-10-runtime-reflection-from-a-class-name/)
 - [Reflecting Annotations in Scala 2.10](http://www.veebsbraindump.com/2013/01/reflecting-annotations-in-scala-2-10/)


