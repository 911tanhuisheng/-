package com.sandbox.dockertest;


import com.github.dockerjava.api.DockerClient;

import com.sandbox.config.DockerHelper;

/**
 * 测试 Docker 连通性 练习docker java操作这个docker
 */
public class DockerTest {

    public static void main(String[] args) throws InterruptedException {
        DockerClient docker = DockerHelper.client();
// 连通性
        docker.pingCmd().exec();
        // 列出容器
        var containers = docker.listContainersCmd().withShowAll(true).exec();
        for (var container : containers) {
            System.out.println(container.getNames()[0]);
        }
        docker.pullImageCmd("nginx:latest")
                .start().awaitCompletion();

        System.out.println("成功");
    }

    }


